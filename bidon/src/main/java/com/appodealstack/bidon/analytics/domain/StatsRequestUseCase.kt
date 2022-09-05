package com.appodealstack.bidon.analytics.domain

import com.appodealstack.bidon.auctions.data.models.RoundStat
import com.appodealstack.bidon.core.errors.BaseResponse

internal interface StatsRequestUseCase {
    suspend operator fun invoke(
        auctionId: String,
        auctionConfigurationId: Int,
        results: List<RoundStat>,
    ): Result<BaseResponse>
}