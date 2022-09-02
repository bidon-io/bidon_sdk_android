package com.appodealstack.bidon.analytics.domain

import com.appodealstack.bidon.analytics.data.models.StatsRequestBody
import com.appodealstack.bidon.config.domain.DataBinderType
import com.appodealstack.bidon.config.domain.databinders.CreateRequestBodyUseCase
import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.core.errors.BaseResponse
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInfo
import com.appodealstack.bidon.utilities.ktor.JsonHttpRequest

internal class StatsRequestUseCaseImpl(
    private val createRequestBody: CreateRequestBodyUseCase
) : StatsRequestUseCase {
    private val binders: List<DataBinderType> = listOf(
        DataBinderType.Device,
        DataBinderType.App,
        DataBinderType.Token,
        DataBinderType.Geo,
        DataBinderType.Session,
        DataBinderType.User,
    )

    override suspend fun request(body: StatsRequestBody): Result<BaseResponse> {
        val requestBody = createRequestBody(
            binders = binders,
            dataKeyName = "stats",
            data = body,
            dataSerializer = StatsRequestBody.serializer()
        )
        return JsonHttpRequest().invoke(
            path = StatsRequestPath,
            body = requestBody,
        ).map { jsonResponse ->
            BidonJson.decodeFromJsonElement(BaseResponse.serializer(), jsonResponse)
        }.onFailure {
            logError(Tag, "Error while sending stats", it)
        }.onSuccess {
            logInfo(Tag, "Stats was sent successfully")
        }
    }
}

private const val StatsRequestPath = "stats"
private const val Tag = "StatsRequestUseCase"