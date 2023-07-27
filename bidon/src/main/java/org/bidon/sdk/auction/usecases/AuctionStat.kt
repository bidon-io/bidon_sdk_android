package org.bidon.sdk.auction.usecases

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.RoundResult
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.Round
import org.bidon.sdk.auction.usecases.models.BiddingResult
import org.bidon.sdk.logs.logging.impl.logInfo
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

    fun addRoundResults(result: RoundResult.Results)
    fun sendAuctionStats(auctionData: AuctionResponse, demandAd: DemandAd)
    fun markAuctionCanceled()
}

private typealias StatRound = org.bidon.sdk.stats.models.Round

internal class AuctionStatImpl(
    private val statsRequest: StatsRequestUseCase,
) : AuctionStat {
    private var auctionStartTs: Long = 0L
    private val scope: CoroutineScope get() = CoroutineScope(SdkDispatchers.IO)

    private var auctionId: String = ""

//    private var winnerNetwork: DemandStat.Network? = null
//    private var winnerBidding: DemandStat.Bidding? = null
//    private val winner: DemandStat?
//        get() {
//            return if (isAuctionCanceled) return null
//            else winnerNetwork ?: winnerBidding
//        }

    private var winner: AuctionResult? = null
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

    override fun addRoundResults(result: RoundResult.Results) {
        // get, sort results + update winner
        // save stats
        val biddingResults = (result.biddingResult as? BiddingResult.FilledAd)?.results.orEmpty()
        val networkResults = result.networkResults

        val roundResults = networkResults + biddingResults

        val roundWinner = roundResults
            .firstOrNull { it.roundStatus == RoundStatus.Successful }
            .takeIf { !isAuctionCanceled }

        val cancelledAdUnits = getCancelledNetworkAd(
            round = result.round,
            networkResults = result.networkResults
        )
        val roundStat = RoundStat(
            auctionId = auctionId,
            roundId = result.round.id,
            pricefloor = result.pricefloor,
            winnerDemandId = roundWinner?.adSource?.demandId,
            winnerEcpm = roundWinner?.adSource?.getStats()?.ecpm,
            demands = result.networkResults.map {
                it.asDemandStat()
            } + cancelledAdUnits,
            bidding = result.biddingResult.asDemandStat()
        )
        statsRounds.add(roundStat)
        updateWinnerIfNeed(roundWinner)
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
                            roundStatusCode = RoundStatus.values().first {
                                it.code == demandStat.roundStatusCode
                            }.getFinalStatus(
                                isWinner = demandStat.demandId == (winner as? AuctionResult.Network)?.adSource?.demandId?.demandId
                            ).code
                        )
                    },
                    bidding = roundStat.bidding?.copy(
                        bids = roundStat.bidding.bids.map { bid ->
                            bid.copy(
                                roundStatusCode = RoundStatus.values().first {
                                    it.code == bid.roundStatusCode
                                }.getFinalStatus(
                                    isWinner = bid.demandId == (winner as? AuctionResult.Bidding)?.adSource?.demandId?.demandId
                                ).code
                            )
                        }
                    )
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
                roundStatusCode = RoundStatus.AuctionCancelled.code,
                demandId = it,
                ecpm = null,
                adUnitId = null,
                fillStartTs = null,
                fillFinishTs = null,
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
                                    roundStatusCode = RoundStatus.AuctionCancelled.code,
                                    demandId = it,
                                    ecpm = null,
                                    adUnitId = null,
                                    fillStartTs = null,
                                    fillFinishTs = null,
                                )
                            },
                            bidding = DemandStat.Bidding(
                                bidStartTs = null,
                                bidFinishTs = null,
                                bids = listOf(
                                    DemandStat.Bidding.Bid(
                                        roundStatusCode = RoundStatus.AuctionCancelled.code,
                                        ecpm = null,
                                        demandId = null,
                                        fillStartTs = null,
                                        fillFinishTs = null,
                                    )
                                )
                            ),
                            winnerEcpm = null,
                            winnerDemandId = null
                        )
                    )
                }
            }
        }
    }

    private fun updateWinnerIfNeed(roundWinner: AuctionResult?) {
        if (roundWinner == null) return
        val currentEcpm = winner?.adSource?.getStats()?.ecpm ?: 0.0
        if (currentEcpm < roundWinner.adSource.getStats().ecpm) {
            this.winner = roundWinner
        }
    }

    private fun AuctionResult.Network.asDemandStat(): DemandStat.Network {
        return when (this) {

            is AuctionResult.Network.Success -> {
                val stat = this.adSource.getStats()
                DemandStat.Network(
                    roundStatusCode = this.roundStatus.code,
                    ecpm = stat.ecpm.takeEcpmIfPossible(this.roundStatus),
                    demandId = stat.demandId.demandId,
                    fillStartTs = stat.fillStartTs,
                    fillFinishTs = stat.fillFinishTs,
                    adUnitId = stat.adUnitId
                )
            }

            is AuctionResult.Network.UnknownAdapter -> {
                DemandStat.Network(
                    roundStatusCode = RoundStatus.UnknownAdapter.code,
                    demandId = adapterName,
                    fillStartTs = null,
                    fillFinishTs = null,
                    ecpm = null,
                    adUnitId = null
                )
            }
        }
    }

    private fun BiddingResult.asDemandStat(): DemandStat.Bidding? {
        return when (this) {
            is BiddingResult.NoBid -> {
                DemandStat.Bidding(
                    bidStartTs = this.serverBiddingStartTs,
                    bidFinishTs = this.serverBiddingFinishTs,
                    bids = listOf(
                        DemandStat.Bidding.Bid(
                            roundStatusCode = RoundStatus.NoBid.code,
                            ecpm = null,
                            demandId = null,
                            fillStartTs = null,
                            fillFinishTs = null,
                        )
                    )
                )
            }

            BiddingResult.Idle -> null

            is BiddingResult.FilledAd -> {
                DemandStat.Bidding(
                    bidStartTs = this.serverBiddingStartTs,
                    bidFinishTs = this.serverBiddingFinishTs,
                    bids = bids.map { bid ->
                        val polled = results.firstOrNull { auctionResult ->
                            bid.demand.id.code == auctionResult.adSource.getStats().demandId.demandId
                        }?.adSource?.getStats()
                        DemandStat.Bidding.Bid(
                            roundStatusCode = polled?.roundStatus?.code ?: RoundStatus.AuctionCancelled.code,
                            ecpm = polled?.ecpm,
                            demandId = polled?.demandId?.demandId,
                            fillStartTs = polled?.fillStartTs,
                            fillFinishTs = polled?.fillFinishTs,
                        )
                    }
                )
            }

            is BiddingResult.ServerBiddingStarted -> {
                DemandStat.Bidding(
                    bidStartTs = this.serverBiddingStartTs,
                    bidFinishTs = null,
                    bids = listOf(
                        DemandStat.Bidding.Bid(
                            roundStatusCode = RoundStatus.AuctionCancelled.code,
                            ecpm = null,
                            demandId = null,
                            fillStartTs = null,
                            fillFinishTs = null,
                        )
                    )
                )
            }

            is BiddingResult.TimeoutReached -> {
                DemandStat.Bidding(
                    bidStartTs = this.serverBiddingStartTs,
                    bidFinishTs = null,
                    bids = listOf(
                        DemandStat.Bidding.Bid(
                            roundStatusCode = RoundStatus.BidTimeoutReached.code,
                            ecpm = null,
                            demandId = null,
                            fillStartTs = null,
                            fillFinishTs = null,
                        )
                    )
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
        return StatsRequestBody(
            auctionId = auctionId,
            auctionConfigurationId = auctionConfigurationId,
            result = getResultBody(auctionStartTs, auctionFinishTs),
            rounds = this.map { stat ->
                StatRound(
                    id = stat.roundId,
                    winnerEcpm = stat.winnerEcpm,
                    winnerDemandId = stat.winnerDemandId?.demandId,
                    pricefloor = stat.pricefloor,
                    demands = stat.demands,
                    bidding = stat.bidding
                )
            }
        )
    }

    private fun getResultBody(
        auctionStartTs: Long,
        auctionFinishTs: Long
    ): ResultBody {
        val isSucceed = winner?.roundStatus == RoundStatus.Successful
        val stat = winner?.adSource?.getStats()
        logInfo(TAG, "isSucceed=$isSucceed, stat: $stat")
        return ResultBody(
            status = when {
                isAuctionCanceled -> RoundStatus.AuctionCancelled.code
                winner?.roundStatus == RoundStatus.Successful -> "SUCCESS"
                else -> "FAIL"
            },
            demandId = stat?.demandId?.demandId.takeIf { isSucceed },
            ecpm = stat?.ecpm.takeIf { isSucceed },
            adUnitId = stat?.adUnitId.takeIf { isSucceed },
            auctionStartTs = auctionStartTs,
            auctionFinishTs = auctionFinishTs,
            roundId = stat?.roundId
        )
    }
}

private const val TAG = "AuctionStat"