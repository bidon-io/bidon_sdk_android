package org.bidon.sdk.stats.usecases

import org.bidon.sdk.ads.AdType
import org.bidon.sdk.stats.RoundStat

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface StatsRequestUseCase {
    suspend operator fun invoke(
        auctionId: String,
        auctionConfigurationId: Int,
        results: List<RoundStat>,
        adType: AdType,
    ): Result<org.bidon.sdk.utils.networking.BaseResponse>
}