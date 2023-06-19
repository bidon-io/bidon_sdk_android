package org.bidon.sdk.auction.usecases

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.AuctionResult.Bidding
import org.bidon.sdk.auction.impl.MaxEcpmAuctionResolver
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.Round
import org.bidon.sdk.stats.models.DemandStat
import org.bidon.sdk.stats.models.RoundStat
import org.bidon.sdk.stats.models.RoundStatus
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

    fun sendAuctionStats(auctionData: AuctionResponse, demandAd: DemandAd, auctionConfigurationId: Int?)
}

internal class AuctionStatImpl(
    private val statsRequest: StatsRequestUseCase,
) : AuctionStat {
    private var auctionStartTs: Long = 0L
    private val scope: CoroutineScope get() = CoroutineScope(SdkDispatchers.IO)

    private var auctionId: String = ""
    private var winner: DemandStat? = null
    private val statsRound = mutableListOf<RoundStat>()

    override fun markAuctionStarted(auctionId: String) {
        this.auctionId = auctionId
        this.auctionStartTs = SystemTimeNow
    }

    override fun addRoundResults(
        round: Round,
        pricefloor: Double,
        roundResults: List<AuctionResult>,
    ) {
        val roundWinner = roundResults.firstOrNull { it.roundStatus == RoundStatus.Successful }
        val networkResults = roundResults.filterIsInstance<AuctionResult.Network>()
        val biddingResult = roundResults.filterIsInstance<Bidding>().firstOrNull()
        val unknownNetworkDemands = networkResults.findUnknownNetworkDemands(round)
        val roundStat = RoundStat(
            auctionId = auctionId,
            roundId = round.id,
            pricefloor = pricefloor,
            winnerDemandId = roundWinner?.adSource?.demandId,
            winnerEcpm = roundWinner?.ecpm,
            demands = networkResults.map {
                it.asDemandStat(pricefloor) as DemandStat.Network
            } + unknownNetworkDemands,
            bidding = biddingResult?.asDemandStatBidding(pricefloor)
        )
        statsRound.add(roundStat)
        updateWinnerIfNeed(roundWinner, pricefloor)
    }

    override fun sendAuctionStats(auctionData: AuctionResponse, demandAd: DemandAd, auctionConfigurationId: Int?) {
        scope.launch(SdkDispatchers.Default) {
            // prepare data
            val roundResults = statsRound.map { roundStat ->
                roundStat.copy(
                    demands = roundStat.demands.map { demandStat ->
                        demandStat.copy(
                            roundStatus = demandStat.roundStatus.getFinalStatus(demandStat == winner)
                        )
                    },
                    bidding = roundStat.bidding?.copy(
                        roundStatus = roundStat.bidding.roundStatus.getFinalStatus(roundStat.bidding == winner)
                    )
                )
            }

            // send data
            statsRequest.invoke(
                auctionId = auctionId,
                auctionConfigurationId = auctionConfigurationId ?: -1,
                results = roundResults,
                demandAd = demandAd,
                auctionStartTs = auctionStartTs,
                auctionFinishTs = SystemTimeNow
            )
        }
    }

    private fun List<AuctionResult.Network>.findUnknownNetworkDemands(round: Round): List<DemandStat.Network> {
        return (round.demandIds - this.map { it.adSource.demandId.demandId }.toSet())
            .takeIf { it.isNotEmpty() }
            ?.map { demandId ->
                DemandStat.Network(
                    roundStatus = RoundStatus.UnknownAdapter,
                    demandId = DemandId(demandId),
                    bidStartTs = null,
                    bidFinishTs = null,
                    fillStartTs = null,
                    fillFinishTs = null,
                    ecpm = null,
                    adUnitId = null
                )
            } ?: emptyList()
    }

    private fun Bidding.asDemandStatBidding(pricefloor: Double): DemandStat.Bidding {
        return when (val biddingResult = this) {
            is Bidding.Success,
            is Bidding.Failure.NoFill -> {
                val stat = biddingResult.adSource.buildBidStatistic()
                DemandStat.Bidding(
                    roundStatus = biddingResult.roundStatus,
                    ecpm = biddingResult.ecpm.takeEcpmIfPossible(biddingResult.roundStatus),
                    demandId = biddingResult.adSource.demandId,
                    bidStartTs = stat.bidStartTs,
                    bidFinishTs = stat.bidFinishTs,
                    fillStartTs = stat.fillStartTs,
                    fillFinishTs = stat.fillFinishTs,
                )
            }

            is Bidding.Failure.NoBid -> {
                DemandStat.Bidding(
                    roundStatus = RoundStatus.NoBid,
                    ecpm = pricefloor,
                    demandId = null,
                    bidStartTs = biddingResult.biddingStartTimeTs,
                    bidFinishTs = biddingResult.biddingFinishTimeTs,
                    fillStartTs = null,
                    fillFinishTs = null,
                )
            }

            Bidding.Failure.TimeoutReached -> {
                DemandStat.Bidding(
                    roundStatus = RoundStatus.NoBid,
                    ecpm = pricefloor,
                    demandId = null,
                    bidStartTs = null,
                    bidFinishTs = null,
                    fillStartTs = null,
                    fillFinishTs = null,
                )
            }
        }
    }

    private fun updateWinnerIfNeed(roundWinner: AuctionResult?, pricefloor: Double) {
        MaxEcpmAuctionResolver
        if (roundWinner?.roundStatus == RoundStatus.Successful) {
            if ((winner?.ecpm ?: 0.0) < roundWinner.ecpm) {
                winner = roundWinner.asDemandStat(pricefloor)
            }
        }
    }

    private fun AuctionResult.asDemandStat(pricefloor: Double): DemandStat {
        return when (this) {
            is Bidding.Success,
            is Bidding.Failure.NoFill -> {
                val stat = this.adSource.buildBidStatistic()
                DemandStat.Bidding(
                    roundStatus = this.roundStatus,
                    ecpm = this.ecpm.takeEcpmIfPossible(this.roundStatus),
                    demandId = this.adSource.demandId,
                    bidStartTs = stat.bidStartTs,
                    bidFinishTs = stat.bidFinishTs,
                    fillStartTs = stat.fillStartTs,
                    fillFinishTs = stat.fillFinishTs,
                )
            }

            is Bidding.Failure.NoBid -> {
                DemandStat.Bidding(
                    roundStatus = RoundStatus.NoBid,
                    ecpm = pricefloor,
                    demandId = null,
                    bidStartTs = this.biddingStartTimeTs,
                    bidFinishTs = this.biddingFinishTimeTs,
                    fillStartTs = null,
                    fillFinishTs = null,
                )
            }

            is Bidding.Failure.TimeoutReached -> {
                DemandStat.Bidding(
                    roundStatus = RoundStatus.BidTimeoutReached,
                    ecpm = pricefloor,
                    demandId = null,
                    bidStartTs = null,
                    bidFinishTs = null,
                    fillStartTs = null,
                    fillFinishTs = null,
                )
            }

            is AuctionResult.Network -> {
                val stat = this.adSource.buildBidStatistic()
                DemandStat.Network(
                    roundStatus = this.roundStatus,
                    ecpm = this.ecpm.takeEcpmIfPossible(this.roundStatus),
                    demandId = this.adSource.demandId,
                    bidStartTs = null,
                    bidFinishTs = null,
                    fillStartTs = stat.fillStartTs,
                    fillFinishTs = stat.fillFinishTs,
                    adUnitId = stat.adUnitId
                )
            }
        }
    }

    private fun RoundStatus.getFinalStatus(isWinner: Boolean): RoundStatus {
        return when {
            isWinner -> RoundStatus.Win
            this == RoundStatus.Successful -> RoundStatus.Loss
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
}