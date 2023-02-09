package com.appodealstack.bidon.data.networking.requests

import com.appodealstack.bidon.data.json.JsonParsers
import com.appodealstack.bidon.data.models.stats.ImpressionRequestBody
import com.appodealstack.bidon.data.networking.BaseResponse
import com.appodealstack.bidon.data.networking.JsonHttpRequest
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.domain.databinders.DataBinderType
import com.appodealstack.bidon.domain.stats.impl.logError
import com.appodealstack.bidon.domain.stats.impl.logInfo
import com.appodealstack.bidon.domain.stats.usecases.SendImpressionRequestUseCase
import com.appodealstack.bidon.view.helper.SdkDispatchers
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
            val baseResponse = JsonParsers.parseOrNull<BaseResponse>(jsonResponse.toString())
            requireNotNull(baseResponse)
        }.onFailure {
            logError(Tag, "Error while sending impression $urlPath", it)
        }.onSuccess {
            logInfo(Tag, "Impression $urlPath was sent successfully")
        }
    }
}

private const val Tag = "ImpressionRequestUseCase"