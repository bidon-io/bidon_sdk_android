package com.appodealstack.bidon.analytics.domain

import com.appodealstack.bidon.analytics.data.models.ImpressionRequestBody
import com.appodealstack.bidon.config.domain.DataBinderType
import com.appodealstack.bidon.config.domain.databinders.CreateRequestBodyUseCase
import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.core.SdkDispatchers
import com.appodealstack.bidon.core.errors.BaseResponse
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInfo
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.utilities.ktor.JsonHttpRequest
import kotlinx.coroutines.withContext

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
    )

    override suspend fun invoke(
        urlPath: String,
        bodyKey: String,
        body: ImpressionRequestBody
    ): Result<BaseResponse> = withContext(SdkDispatchers.IO) {
        val requestBody = createRequestBody.invoke(
            binders = binders,
            dataKeyName = bodyKey,
            data = body,
            dataSerializer = ImpressionRequestBody.serializer(),
        )
        logInfo(Tag, "Request body: $requestBody")

        get<JsonHttpRequest>().invoke(
            path = urlPath,
            body = requestBody,
        ).map { jsonResponse ->
            BidonJson.decodeFromJsonElement(BaseResponse.serializer(), jsonResponse)
        }.onFailure {
            logError(Tag, "Error while sending impression $urlPath", it)
        }.onSuccess {
            logInfo(Tag, "Impression $urlPath was sent successfully")
        }
    }
}

private const val Tag = "ImpressionRequestUseCase"