package org.bidon.sdk.auction.usecases.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.auction.AuctionResolver
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.auction.models.RoundRequest
import org.bidon.sdk.auction.usecases.AuctionStat
import org.bidon.sdk.auction.usecases.models.BiddingResult
import org.bidon.sdk.auction.usecases.models.RoundResult
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.models.DemandStat
import org.bidon.sdk.stats.models.ResultBody
import org.bidon.sdk.stats.models.RoundStat
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.models.StatsRequestBody
import org.bidon.sdk.stats.usecases.StatsRequestUseCase
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.ext.SystemTimeNow

private typealias StatRound = org.bidon.sdk.stats.models.Round

internal class AuctionStatImpl(
    private val statsRequest: StatsRequestUseCase,
    private val resolver: AuctionResolver
) : AuctionStat {
    private var auctionStartTs: Long = 0L
    private val scope: CoroutineScope get() = CoroutineScope(SdkDispatchers.IO)

    private var auctionId: String = ""

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

    override suspend fun addRoundResults(result: RoundResult.Results): List<RoundStat> {
        // get, sort results + update winner
        // save stats
        val biddingResults = (result.biddingResult as? BiddingResult.FilledAd)?.results.orEmpty()
        val networkResults = result.networkResults

        val roundResults = resolver.sortWinners(networkResults + biddingResults)

        val roundWinner = roundResults
            .firstOrNull { it.roundStatus == RoundStatus.Successful }
            .takeIf { !isAuctionCanceled }

        val roundStat = RoundStat(
            auctionId = auctionId,
            roundId = result.round.id,
            pricefloor = result.pricefloor,
            winnerDemandId = roundWinner?.adSource?.demandId,
            winnerEcpm = roundWinner?.adSource?.getStats()?.ecpm,
            demands = result.asDemandStatNetworks(),
            bidding = result.asDemandStatBidding()
        )
        statsRounds.add(roundStat)
        updateWinnerIfNeed(roundWinner)
        return statsRounds
    }

    override fun sendAuctionStats(auctionData: AuctionResponse, demandAd: DemandAd): StatsRequestBody {
        // prepare data
        val canceledRounds = getNotConductedRoundStats(
            rounds = auctionData.rounds.orEmpty(),
            completedRoundIds = statsRounds.map { it.roundId },
        )
        val roundResults = statsRounds.map { roundStat ->
            roundStat.copy(
                demands = roundStat.demands.map { demandStat ->
                    demandStat.copy(
                        roundStatusCode = RoundStatus.values().first {
                            it.code == demandStat.roundStatusCode
                        }.getFinalStatus(
                            isWinner = demandStat.demandId == (winner as? AuctionResult.Network)?.adSource?.demandId?.demandId &&
                                demandStat.adUnitId == (winner as? AuctionResult.Network)?.adSource?.getStats()?.adUnitId &&
                                demandStat.ecpm == (winner as? AuctionResult.Network)?.adSource?.getStats()?.ecpm
                        ).code
                    )
                },
                bidding = roundStat.bidding?.copy(
                    bids = roundStat.bidding.bids.map { bid ->
                        bid.copy(
                            roundStatusCode = RoundStatus.values().first {
                                it.code == bid.roundStatusCode
                            }.getFinalStatus(
                                isWinner = bid.demandId != null &&
                                    bid.demandId == (winner as? AuctionResult.Bidding)?.adSource?.demandId?.demandId &&
                                    bid.ecpm == (winner as? AuctionResult.Bidding)?.adSource?.getStats()?.ecpm
                            ).code
                        )
                    }
                )
            )
        } + canceledRounds

        // send data
        val statsRequestBody = roundResults.asStatsRequestBody(
            auctionId = auctionId,
            auctionConfigurationId = auctionData.auctionConfigurationId ?: -1,
            auctionStartTs = auctionStartTs,
            auctionFinishTs = SystemTimeNow
        )
        scope.launch(SdkDispatchers.Default) {
            statsRequest.invoke(
                statsRequestBody = statsRequestBody,
                demandAd = demandAd,
            )
        }
        return statsRequestBody
    }

    private fun RoundResult.Results.asDemandStatNetworks(): List<DemandStat.Network> {
        val result: RoundResult.Results = this
        val cancelledAdUnits = getCancelledDemands(
            round = result.round,
            networkResults = result.networkResults
        )
        return result.networkResults.map { it.asDemandStatNetwork() } + cancelledAdUnits
    }

    private fun getCancelledDemands(
        round: RoundRequest,
        networkResults: List<AuctionResult>
    ): List<DemandStat.Network> {
        if (!isAuctionCanceled) return emptyList()
        val cancelledDemandIds = round.demandIds - networkResults.map {
            when (it) {
                is AuctionResult.Network -> it.adSource.demandId.demandId
                is AuctionResult.Bidding -> it.adSource.demandId.demandId
                is AuctionResult.UnknownAdapter -> it.adapterName
            }
        }.toSet()
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

    private fun getNotConductedRoundStats(
        rounds: List<RoundRequest>,
        completedRoundIds: List<String>,
    ): List<RoundStat> {
        return buildList {
            rounds.forEach { round: RoundRequest ->
                if (round.id !in completedRoundIds) {
                    add(
                        RoundStat(
                            auctionId = auctionId,
                            roundId = round.id,
                            pricefloor = null,
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
                            bidding = if (round.biddingIds.isNotEmpty()) {
                                DemandStat.Bidding(
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
                                )
                            } else {
                                null
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
        if (roundWinner == null) return
        val currentEcpm = winner?.adSource?.getStats()?.ecpm ?: 0.0
        if (currentEcpm < roundWinner.adSource.getStats().ecpm) {
            this.winner = roundWinner
        }
    }

    private fun AuctionResult.asDemandStatNetwork(): DemandStat.Network {
        return when (this) {
            is AuctionResult.Network -> {
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

            is AuctionResult.UnknownAdapter -> {
                DemandStat.Network(
                    roundStatusCode = RoundStatus.UnknownAdapter.code,
                    demandId = adapterName,
                    fillStartTs = null,
                    fillFinishTs = null,
                    ecpm = null,
                    adUnitId = null
                )
            }

            is AuctionResult.Bidding -> error("unexpected")
        }
    }

    private fun RoundResult.Results.asDemandStatBidding(): DemandStat.Bidding? {
        val demandError: (RoundStatus) -> DemandStat.Bidding.Bid = {
            DemandStat.Bidding.Bid(
                roundStatusCode = it.code,
                ecpm = null,
                demandId = null,
                fillStartTs = null,
                fillFinishTs = null,
            )
        }

        return when (val br = this.biddingResult) {
            BiddingResult.Idle -> null

            is BiddingResult.NoBid -> {
                DemandStat.Bidding(
                    bidStartTs = br.serverBiddingStartTs,
                    bidFinishTs = br.serverBiddingFinishTs,
                    bids = listOf(demandError(RoundStatus.NoBid))
                )
            }

            is BiddingResult.FilledAd -> {
                DemandStat.Bidding(
                    bidStartTs = br.serverBiddingStartTs,
                    bidFinishTs = br.serverBiddingFinishTs,
                    bids = br.results.map { auctionResult ->

                        when (auctionResult) {
                            is AuctionResult.Bidding -> {
                                val bid = br.bids.first { it.demandId == auctionResult.adSource.demandId.demandId }
                                val stat = auctionResult.adSource.getStats()
                                DemandStat.Bidding.Bid(
                                    roundStatusCode = auctionResult.roundStatus.code,
                                    ecpm = bid.price,
                                    demandId = bid.demandId ?: stat.demandId.demandId,
                                    fillStartTs = stat.fillStartTs,
                                    fillFinishTs = stat.fillFinishTs,
                                )
                            }

                            is AuctionResult.UnknownAdapter -> {
                                DemandStat.Bidding.Bid(
                                    roundStatusCode = RoundStatus.UnknownAdapter.code,
                                    demandId = auctionResult.adapterName,
                                    fillStartTs = null,
                                    fillFinishTs = null,
                                    ecpm = null,
                                )
                            }

                            is AuctionResult.Network -> error("unexpected")
                        }
                    }
                )
            }

            is BiddingResult.ServerBiddingStarted -> {
                DemandStat.Bidding(
                    bidStartTs = br.serverBiddingStartTs,
                    bidFinishTs = null,
                    bids = listOf(demandError(RoundStatus.AuctionCancelled))
                )
            }

            is BiddingResult.TimeoutReached -> {
                DemandStat.Bidding(
                    bidStartTs = br.serverBiddingStartTs,
                    bidFinishTs = br.serverBiddingFinishTs,
                    bids = listOf(
                        demandError(
                            RoundStatus.BidTimeoutReached.takeIf { br.serverBiddingFinishTs == null }
                                ?: RoundStatus.FillTimeoutReached
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
