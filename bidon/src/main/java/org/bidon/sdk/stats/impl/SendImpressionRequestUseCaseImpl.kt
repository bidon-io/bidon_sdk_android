package org.bidon.sdk.stats.impl

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
import kotlinx.coroutines.withContext

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class SendImpressionRequestUseCaseImpl(
    private val createRequestBody: CreateRequestBodyUseCase,
) : SendImpressionRequestUseCase {

    private val binders: List<DataBinderType> = listOf(
        DataBinderType.Device,
        DataBinderType.Session,
        DataBinderType.App,
        DataBinderType.User,
        DataBinderType.Geo,
        DataBinderType.Token,
        DataBinderType.Segment,
    )

    override suspend fun invoke(
        urlPath: String,
        bodyKey: String,
        body: ImpressionRequestBody,
    ): Result<BaseResponse> = withContext(SdkDispatchers.IO) {
        val requestBody = createRequestBody.invoke(
            binders = binders,
            dataKeyName = bodyKey,
            data = body,
        )
        logInfo(Tag, "Request body: $requestBody")

        get<JsonHttpRequest>().invoke(
            path = urlPath,
            body = requestBody,
        ).mapCatching { jsonResponse ->
            val baseResponse = JsonParsers.parseOrNull<BaseResponse>(jsonResponse)
            requireNotNull(baseResponse)
        }.onFailure {
            logError(Tag, "Error while sending impression $urlPath", it)
        }.onSuccess {
            logInfo(Tag, "Impression $urlPath was sent successfully")
        }
    }
}

private const val Tag = "ImpressionRequestUseCase"
