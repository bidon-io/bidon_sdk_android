package com.appodealstack.bidon.analytics.domain

import com.appodealstack.bidon.analytics.data.models.StatsRequestBody
import com.appodealstack.bidon.core.errors.BaseResponse

internal interface StatsRequestUseCase {
    suspend fun request(body: StatsRequestBody): Result<BaseResponse>
}