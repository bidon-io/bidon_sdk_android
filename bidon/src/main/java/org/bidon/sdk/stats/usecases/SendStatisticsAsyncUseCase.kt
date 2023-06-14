package org.bidon.sdk.stats.usecases

import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.stats.models.RoundStat
import org.bidon.sdk.stats.models.RoundStatus

/**
 * Created by Aleksei Cherniaev on 05/06/2023.
 */
@Deprecated("")
internal interface SendStatisticsAsyncUseCase {
    operator fun invoke(
        demandAd: DemandAd,
        auctionResponse: AuctionResponse,
        auctionStartTs: Long,
        auctionFinishTs: Long,
        statsAuctionResults: List<AuctionResult>,
        statsRound: List<RoundStat>,
    )

    companion object {
        fun Double?.takeEcpmIfPossible(status: RoundStatus): Double? {
            return this?.takeIf {
                status !in arrayOf(
                    RoundStatus.NoBid,
                    RoundStatus.NoAppropriateAdUnitId
                )
            }
        }
    }
}
