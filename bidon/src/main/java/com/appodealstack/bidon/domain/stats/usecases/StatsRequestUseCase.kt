package com.appodealstack.bidon.domain.stats.usecases

import com.appodealstack.bidon.data.networking.BaseResponse
import com.appodealstack.bidon.domain.common.AdType
import com.appodealstack.bidon.domain.stats.RoundStat

internal interface StatsRequestUseCase {
    suspend operator fun invoke(
        auctionId: String,
        auctionConfigurationId: Int,
        results: List<RoundStat>,
        adType: AdType,
    ): Result<BaseResponse>
}