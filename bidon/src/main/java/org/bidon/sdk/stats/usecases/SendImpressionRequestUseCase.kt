package org.bidon.sdk.stats.usecases

import org.bidon.sdk.stats.models.ImpressionRequestBody
import org.bidon.sdk.utils.networking.BaseResponse
/**
 * Created by Bidon Team on 06/02/2023.
 */
internal interface SendImpressionRequestUseCase {
    suspend operator fun invoke(
        urlPath: String,
        bodyKey: String,
        body: ImpressionRequestBody,
        extras: Map<String, Any>
    ): Result<BaseResponse>

    enum class Type(val key: String) {
        Show("show"),
        Click("click"),
        Reward("reward"),
    }
}