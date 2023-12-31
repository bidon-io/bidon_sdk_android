package org.bidon.sdk.stats.impl

import kotlinx.coroutines.withContext
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.databinders.DataBinderType
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.models.ImpressionRequestBody
import org.bidon.sdk.stats.usecases.SendImpressionRequestUseCase
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.json.JsonParsers
import org.bidon.sdk.utils.networking.BaseResponse
import org.bidon.sdk.utils.networking.JsonHttpRequest
import org.bidon.sdk.utils.networking.requests.CreateRequestBodyUseCase

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal class SendImpressionRequestUseCaseImpl(
    private val createRequestBody: CreateRequestBodyUseCase,
) : SendImpressionRequestUseCase {

    private val binders: List<DataBinderType> = listOf(
        DataBinderType.Device,
        DataBinderType.Session,
        DataBinderType.App,
        DataBinderType.User,
        DataBinderType.Token,
        DataBinderType.Segment,
        DataBinderType.Reg,
        DataBinderType.Test,
    )

    override suspend fun invoke(
        urlPath: String,
        bodyKey: String,
        body: ImpressionRequestBody,
        extras: Map<String, Any>
    ): Result<BaseResponse> = withContext(SdkDispatchers.IO) {
        val requestBody = createRequestBody(
            binders = binders,
            dataKeyName = bodyKey,
            data = body,
            extras = BidonSdk.getExtras() + extras
        )
        logInfo(TAG, "Request body: $requestBody")

        get<JsonHttpRequest>().invoke(
            path = urlPath,
            body = requestBody,
        ).mapCatching { jsonResponse ->
            val baseResponse = JsonParsers.parseOrNull<BaseResponse>(jsonResponse)
            requireNotNull(baseResponse)
        }.onFailure {
            logError(TAG, "Error while sending impression $urlPath", it)
        }.onSuccess {
            logInfo(TAG, "Impression $urlPath was sent successfully")
        }
    }
}

private const val TAG = "ImpressionRequestUseCase"
