package org.bidon.sdk.auction.usecases

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.AuctionResult.Bidding
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.Round
import org.bidon.sdk.stats.impl.asSuccessResultOrFail
import org.bidon.sdk.stats.models.Demand
import org.bidon.sdk.stats.models.DemandStat
import org.bidon.sdk.stats.models.ResultBody
import org.bidon.sdk.stats.models.RoundStat
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.models.StatsRequestBody
import org.bidon.sdk.stats.usecases.StatsRequestUseCase
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.ext.SystemTimeNow

/**
 * Created by Aleksei Cherniaev on 09/06/2023.
 */
internal interface AuctionStat {
    fun markAuctionStarted(auctionId: String)

    fun addRoundResults(
        round: Round,
        pricefloor: Double,
        roundResults: List<AuctionResult>,
    )

    fun sendAuctionStats(auctionData: AuctionResponse, demandAd: DemandAd)
    fun markAuctionCanceled()
}

private typealias StatRound = org.bidon.sdk.stats.models.Round
private typealias StatBidding = org.bidon.sdk.stats.models.Bidding

internal class AuctionStatImpl(
    private val statsRequest: StatsRequestUseCase,
) : AuctionStat {
    private var auctionStartTs: Long = 0L
    private val scope: CoroutineScope get() = CoroutineScope(SdkDispatchers.IO)

    private var auctionId: String = ""
    private var winner: DemandStat? = null
        get() {
            return if (isAuctionCanceled) return null
            else field
        }

    private val statsRounds = mutableListOf<RoundStat>()
    private var isAuctionCanceled = false

    override fun markAuctionStarted(auctionId: String) {
        this.auctionId = auctionId
        this.auctionStartTs = SystemTimeNow
    }

    override fun markAuctionCanceled() {
        isAuctionCanceled = true
    }

    override fun addRoundResults(
        round: Round,
        pricefloor: Double,
        roundResults: List<AuctionResult>,
    ) {
        val roundWinner = roundResults.firstOrNull { it.roundStatus == RoundStatus.Successful }.takeIf { !isAuctionCanceled }
        val biddingResult = roundResults.filterIsInstance<Bidding>()
        val networkResults = roundResults.filterIsInstance<AuctionResult.Network>()
        val cancelledAdUnits = getCancelledNetworkAd(
            round = round,
            networkResults = networkResults
        )
        val cancelledBidding = getCancelledBidding(round.biddingIds, isAuctionCanceled)?.let(::listOf).orEmpty()
        val roundStat = RoundStat(
            auctionId = auctionId,
            roundId = round.id,
            pricefloor = pricefloor,
            winnerDemandId = roundWinner?.adSource?.demandId,
            winnerEcpm = roundWinner?.adSource?.getStats()?.ecpm,
            demands = networkResults.map {
                it.asDemandStat() as DemandStat.Network
            } + cancelledAdUnits,
            bidding = biddingResult.map {
                it.asDemandStatBidding()
            } + cancelledBidding,
        )
        statsRounds.add(roundStat)
        updateWinnerIfNeed(roundWinner)
    }

    private fun getCancelledBidding(biddingIds: List<String>, auctionCanceled: Boolean): DemandStat.Bidding? {
        if (biddingIds.isNotEmpty() && auctionCanceled) {
            return DemandStat.Bidding(
                demandId = null,
                bidFinishTs = null,
                fillFinishTs = null,
                bidStartTs = null,
                ecpm = null,
                fillStartTs = null,
                roundStatus = RoundStatus.AuctionCancelled
            )
        }
        return null
    }

    override fun sendAuctionStats(auctionData: AuctionResponse, demandAd: DemandAd) {
        scope.launch(SdkDispatchers.Default) {
            // prepare data
            val canceledRounds = getCanceledRoundStats(
                rounds = auctionData.rounds.orEmpty(),
                completedRoundIds = statsRounds.map { it.roundId },
                isCanceled = isAuctionCanceled
            )
            val roundResults = statsRounds.map { roundStat ->
                roundStat.copy(
                    demands = roundStat.demands.map { demandStat ->
                        demandStat.copy(
                            roundStatus = demandStat.roundStatus.getFinalStatus(demandStat == winner)
                        )
                    },
                    bidding = roundStat.bidding.map {
                        it.copy(
                            roundStatus = it.roundStatus.getFinalStatus(roundStat.bidding == winner)
                        )
                    }
                )
            } + canceledRounds

            // send data
            statsRequest.invoke(
                statsRequestBody = roundResults.asStatsRequestBody(
                    auctionId = auctionId,
                    auctionConfigurationId = auctionData.auctionConfigurationId ?: -1,
                    auctionStartTs = auctionStartTs,
                    auctionFinishTs = SystemTimeNow
                ),
                demandAd = demandAd,
            )
        }
    }

    private fun getCancelledNetworkAd(
        round: Round,
        networkResults: List<AuctionResult.Network>
    ): List<DemandStat.Network> {
        val cancelledDemandIds = if (isAuctionCanceled) {
            round.demandIds - networkResults.map {
                when (it) {
                    is AuctionResult.Network.Success -> it.adSource.demandId.demandId
                    is AuctionResult.Network.UnknownAdapter -> it.adapterName
                }
            }.toSet()
        } else emptyList()
        return cancelledDemandIds.map {
            DemandStat.Network(
                roundStatus = RoundStatus.AuctionCancelled,
                demandId = DemandId(it),
                ecpm = null,
                adUnitId = null,
                fillStartTs = null,
                bidStartTs = null,
                fillFinishTs = null,
                bidFinishTs = null
            )
        }
    }

    private fun getCanceledRoundStats(
        rounds: List<Round>,
        completedRoundIds: List<String>,
        isCanceled: Boolean
    ): List<RoundStat> {
        if (!isCanceled) return emptyList()
        return buildList {
            rounds.forEach { round: Round ->
                if (round.id !in completedRoundIds) {
                    add(
                        RoundStat(
                            auctionId = auctionId,
                            roundId = round.id,
                            pricefloor = 0.0,
                            demands = round.demandIds.map {
                                DemandStat.Network(
                                    roundStatus = RoundStatus.AuctionCancelled,
                                    demandId = DemandId(it),
                                    ecpm = null,
                                    adUnitId = null,
                                    fillStartTs = null,
                                    bidStartTs = null,
                                    fillFinishTs = null,
                                    bidFinishTs = null
                                )
                            },
                            bidding = round.biddingIds.map {
                                DemandStat.Bidding(
                                    roundStatus = RoundStatus.AuctionCancelled,
                                    ecpm = null,
                                    demandId = null,
                                    bidStartTs = null,
                                    bidFinishTs = null,
                                    fillStartTs = null,
                                    fillFinishTs = null,
                                )
                            },
                            winnerEcpm = null,
                            winnerDemandId = null
                        )
                    )
                }
            }
        }
    }

    private fun updateWinnerIfNeed(roundWinner: AuctionResult?) {
        if (roundWinner?.roundStatus == RoundStatus.Successful) {
            if ((winner?.ecpm ?: 0.0) < roundWinner.adSource.getStats().ecpm) {
                winner = roundWinner.asDemandStat()
            }
        }
    }

    private fun AuctionResult.asDemandStat(): DemandStat {
        return when (this) {
            is Bidding -> this.asDemandStatBidding()

            is AuctionResult.Network.Success -> {
                val stat = this.adSource.getStats()
                DemandStat.Network(
                    roundStatus = this.roundStatus,
                    ecpm = stat.ecpm.takeEcpmIfPossible(this.roundStatus),
                    demandId = stat.demandId,
                    bidStartTs = null,
                    bidFinishTs = null,
                    fillStartTs = stat.fillStartTs,
                    fillFinishTs = stat.fillFinishTs,
                    adUnitId = stat.adUnitId
                )
            }

            is AuctionResult.Network.UnknownAdapter -> {
                DemandStat.Network(
                    roundStatus = RoundStatus.UnknownAdapter,
                    demandId = DemandId(this.adapterName),
                    bidStartTs = null,
                    bidFinishTs = null,
                    fillStartTs = null,
                    fillFinishTs = null,
                    ecpm = null,
                    adUnitId = null
                )
            }
        }
    }

    private fun Bidding.asDemandStatBidding(): DemandStat.Bidding {
        return when (val biddingResult = this) {
            is Bidding.Success,
            is Bidding.Failure.Other -> {
                val stat = biddingResult.adSource.getStats()
                DemandStat.Bidding(
                    roundStatus = biddingResult.roundStatus,
                    ecpm = stat.ecpm.takeEcpmIfPossible(biddingResult.roundStatus),
                    demandId = stat.demandId,
                    bidStartTs = stat.bidStartTs,
                    bidFinishTs = stat.bidFinishTs,
                    fillStartTs = stat.fillStartTs,
                    fillFinishTs = stat.fillFinishTs,
                )
            }

            is Bidding.Failure.NoBid -> {
                DemandStat.Bidding(
                    roundStatus = RoundStatus.NoBid,
                    ecpm = null,
                    demandId = null,
                    bidStartTs = biddingResult.biddingStartTimeTs,
                    bidFinishTs = biddingResult.biddingFinishTimeTs,
                    fillStartTs = null,
                    fillFinishTs = null,
                )
            }
        }
    }

    private fun RoundStatus.getFinalStatus(isWinner: Boolean): RoundStatus {
        return when {
            isWinner -> RoundStatus.Win
            this == RoundStatus.Successful -> RoundStatus.Lose
            else -> this
        }
    }

    private fun Double?.takeEcpmIfPossible(status: RoundStatus): Double? {
        return this?.takeIf {
            status !in arrayOf(
                RoundStatus.NoBid,
                RoundStatus.NoAppropriateAdUnitId
            )
        }
    }

    private fun List<RoundStat>.asStatsRequestBody(
        auctionId: String,
        auctionConfigurationId: Int,
        auctionStartTs: Long,
        auctionFinishTs: Long,
    ): StatsRequestBody {
        val cancelledOrWinner = findCancelledOrWinnerOrNull(auctionStartTs, auctionFinishTs)
        return StatsRequestBody(
            auctionId = auctionId,
            auctionConfigurationId = auctionConfigurationId,
            result = cancelledOrWinner,
            rounds = this.map { stat ->
                StatRound(
                    id = stat.roundId,
                    winnerEcpm = stat.winnerEcpm,
                    winnerDemandId = stat.winnerDemandId?.demandId,
                    pricefloor = stat.pricefloor,
                    demands = stat.demands.map { demandStat ->
                        Demand(
                            demandId = demandStat.demandId.demandId,
                            adUnitId = demandStat.adUnitId,
                            roundStatusCode = demandStat.roundStatus.code,
                            ecpm = demandStat.ecpm,
                            bidStartTs = demandStat.bidStartTs,
                            bidFinishTs = demandStat.bidFinishTs,
                            fillStartTs = demandStat.fillStartTs,
                            fillFinishTs = demandStat.fillFinishTs,
                        )
                    },
                    biddings = stat.bidding.map {
                        StatBidding(
                            demandId = it.demandId?.demandId,
                            bidFinishTs = it.bidFinishTs,
                            fillFinishTs = it.fillFinishTs,
                            bidStartTs = it.bidStartTs,
                            ecpm = it.ecpm,
                            fillStartTs = it.fillStartTs,
                            roundStatusCode = it.roundStatus.code
                        )
                    }
                )
            }
        )
    }

    private fun List<RoundStat>.findCancelledOrWinnerOrNull(
        auctionStartTs: Long,
        auctionFinishTs: Long
    ): ResultBody {
        val results = this
            .flatMap { it.demands + it.bidding }
        val cancelled = results.firstOrNull { it.roundStatus == RoundStatus.AuctionCancelled }
        val winner = results.firstOrNull { it.roundStatus == RoundStatus.Win }
        return (cancelled ?: winner).asSuccessResultOrFail(
            auctionStartTs = auctionStartTs,
            auctionFinishTs = auctionFinishTs
        )
    }
}