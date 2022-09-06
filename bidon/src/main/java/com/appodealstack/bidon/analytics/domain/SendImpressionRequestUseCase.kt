package com.appodealstack.bidon.analytics.domain

import com.appodealstack.bidon.analytics.data.models.ImpressionRequestBody
import com.appodealstack.bidon.core.errors.BaseResponse

internal interface SendImpressionRequestUseCase {
    suspend operator fun invoke(
        type: Type,
        body: ImpressionRequestBody,
    ): Result<BaseResponse>

    enum class Type(val key: String) {
        Show("show"),
        Click("click")
    }
}