package com.appodealstack.bidon.config.data.impl

import com.appodealstack.bidon.config.data.models.ConfigRequestBody
import com.appodealstack.bidon.config.data.models.ConfigResponse
import com.appodealstack.bidon.config.domain.DataBinderType
import com.appodealstack.bidon.config.domain.GetConfigRequestUseCase
import com.appodealstack.bidon.config.domain.databinders.CreateRequestBodyUseCase
import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.utilities.ktor.JsonHttpRequest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.putJsonObject

internal class GetConfigRequestUseCaseImpl(
    private val createRequestBody: CreateRequestBodyUseCase
) : GetConfigRequestUseCase {
    private val binders: List<DataBinderType> = listOf(
        DataBinderType.Device,
        DataBinderType.App,
        DataBinderType.Token,
        DataBinderType.Geo,
        DataBinderType.Session,
        DataBinderType.User,
    )

    override suspend fun request(body: ConfigRequestBody): Result<ConfigResponse> {
        val bindersData = createRequestBody(
            binders = binders,
            dataKeyName = null,
            data = null,
            dataSerializer = null,
        )
        val requestBody = buildJsonObject {
            put("adapters", BidonJson.encodeToJsonElement(body.adapters))
            bindersData.forEach { (key, jsonObject) ->
                put(key, jsonObject)
            }
        }
        return get<JsonHttpRequest>().invoke(
            path = ConfigRequestPath,
            body = requestBody,
        ).map { jsonResponse ->
            val config = jsonResponse.getValue("init")
            BidonJson.decodeFromJsonElement(ConfigResponse.serializer(), config)
        }
    }
}

private const val ConfigRequestPath = "config"