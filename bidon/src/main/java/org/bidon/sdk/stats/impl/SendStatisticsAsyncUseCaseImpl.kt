package org.bidon.sdk.stats.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.models.DemandStat
import org.bidon.sdk.stats.models.RoundStat
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.usecases.SendStatisticsAsyncUseCase
import org.bidon.sdk.stats.usecases.SendStatisticsAsyncUseCase.Companion.takeEcpmIfPossible
import org.bidon.sdk.stats.usecases.StatsRequestUseCase
import org.bidon.sdk.utils.SdkDispatchers

internal class SendStatisticsAsyncUseCaseImpl(
    private val statsRequest: StatsRequestUseCase,
) : SendStatisticsAsyncUseCase {

    private val scope: CoroutineScope get() = CoroutineScope(SdkDispatchers.IO)

    override fun invoke(
        demandAd: DemandAd,
        auctionResponse: AuctionResponse,
        auctionStartTs: Long,
        auctionFinishTs: Long,
        statsAuctionResults: List<AuctionResult>,
        statsRound: List<RoundStat>,
    ) {
        scope.launch(SdkDispatchers.Default) {
            val networksBidStats = statsAuctionResults.filterIsInstance<AuctionResult.Network>().map {
                (it.adSource as StatisticsCollector).buildBidStatistic()
            }
            // prepare data
            val roundResults = statsRound.map { roundStat ->
                val errorDemandStat = roundStat.demands
                val succeedDemandStat = networksBidStats.filter { it.roundId == roundStat.roundId }
                    .map { bidStat ->
                        val status = requireNotNull(bidStat.roundStatus?.toSuccessfulOrItself())
                        DemandStat.Network(
                            roundStatus = status,
                            demandId = bidStat.demandId,
                            bidStartTs = bidStat.bidStartTs,
                            bidFinishTs = bidStat.bidFinishTs,
                            fillStartTs = bidStat.fillStartTs,
                            fillFinishTs = bidStat.fillFinishTs,
                            ecpm = bidStat.ecpm.takeEcpmIfPossible(status),
                            adUnitId = bidStat.adUnitId
                        )
                    }
                roundStat.copy(
                    demands = (succeedDemandStat + errorDemandStat).map { demandStat ->
                        demandStat.copy(
                            roundStatus = demandStat.roundStatus.toSuccessfulOrItself()
                        )
                    },
                    bidding = roundStat.bidding?.copy(
//                        roundStatus = roundStat.bidding.roundStatus.toSuccessfulOrItself()
                    )
                )
            }

            // send data
            statsRequest.invoke(
                auctionId = auctionResponse.auctionId ?: "",
                auctionConfigurationId = auctionResponse.auctionConfigurationId ?: -1,
                results = roundResults,
                demandAd = demandAd,
                auctionStartTs = auctionStartTs,
                auctionFinishTs = auctionFinishTs
            )
        }
    }

    private fun RoundStatus.toSuccessfulOrItself(): RoundStatus {
        return if (this == RoundStatus.Successful) {
            RoundStatus.Loss
        } else {
            this
        }
    }
}