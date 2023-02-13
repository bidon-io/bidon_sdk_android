package com.appodealstack.bidon.stats.impl

import com.appodealstack.bidon.databinders.DataBinderType
import com.appodealstack.bidon.logs.logging.impl.logError
import com.appodealstack.bidon.logs.logging.impl.logInfo
import com.appodealstack.bidon.stats.models.ImpressionRequestBody
import com.appodealstack.bidon.stats.usecases.SendImpressionRequestUseCase
import com.appodealstack.bidon.utils.SdkDispatchers
import com.appodealstack.bidon.utils.di.get
import com.appodealstack.bidon.utils.json.JsonParsers
import com.appodealstack.bidon.utils.networking.BaseResponse
import com.appodealstack.bidon.utils.networking.JsonHttpRequest
import com.appodealstack.bidon.utils.networking.requests.CreateRequestBodyUseCase
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
            dataSerializer = JsonParsers.getSerializer(),
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
