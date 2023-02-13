package com.appodealstack.bidon.stats.usecases

import com.appodealstack.bidon.stats.models.ImpressionRequestBody
import com.appodealstack.bidon.utils.networking.BaseResponse
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