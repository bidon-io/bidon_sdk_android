package org.bidon.sdk.stats.usecases

import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.stats.models.RoundStat
import org.bidon.sdk.utils.networking.BaseResponse

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface StatsRequestUseCase {
    suspend operator fun invoke(
        auctionId: String,
        auctionConfigurationId: Int,
        auctionStartTs: Long,
        auctionFinishTs: Long,
        results: List<RoundStat>,
        demandAd: DemandAd,
    ): Result<BaseResponse>
}