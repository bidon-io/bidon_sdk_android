package com.appodealstack.bidon.stats.usecases

import com.appodealstack.bidon.ads.AdType
import com.appodealstack.bidon.stats.RoundStat

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface StatsRequestUseCase {
    suspend operator fun invoke(
        auctionId: String,
        auctionConfigurationId: Int,
        results: List<RoundStat>,
        adType: AdType,
    ): Result<com.appodealstack.bidon.utils.networking.BaseResponse>
}