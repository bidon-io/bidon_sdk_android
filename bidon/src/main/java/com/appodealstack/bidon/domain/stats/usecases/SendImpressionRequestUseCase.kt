package com.appodealstack.bidon.domain.stats.usecases

import com.appodealstack.bidon.data.models.stats.ImpressionRequestBody
import com.appodealstack.bidon.data.networking.BaseResponse
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface SendImpressionRequestUseCase {
    suspend operator fun invoke(
        urlPath: String,
        bodyKey: String,
        body: ImpressionRequestBody,
    ): Result<BaseResponse>

    enum class Type(val key: String) {
        Show("show"),
        Click("click"),
        Reward("reward"),
    }
}