package com.appodealstack.bidon.config.data.impl

import com.appodealstack.bidon.config.data.models.ConfigRequestBody
import com.appodealstack.bidon.config.data.models.ConfigResponse
import com.appodealstack.bidon.config.domain.DataBinderType
import com.appodealstack.bidon.config.domain.DataProvider
import com.appodealstack.bidon.config.domain.GetConfigRequestUseCase
import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.core.ext.logInfo
import com.appodealstack.bidon.utilities.keyvaluestorage.KeyValueStorage
import com.appodealstack.bidon.utilities.ktor.JsonHttpRequest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement

internal class GetConfigRequestUseCaseImpl(
    private val dataProvider: DataProvider,
    private val keyValueStorage: KeyValueStorage
) : GetConfigRequestUseCase {
    private val binders: List<DataBinderType> = listOf(DataBinderType.Device, DataBinderType.App)

    override suspend fun request(body: ConfigRequestBody): Result<ConfigResponse> {
        val bindData = dataProvider.provide(binders)
        val requestBody = buildJsonObject {
            put("adapters", BidonJson.encodeToJsonElement(body.adapters))
            bindData.forEach { (key, jsonElement) ->
                put(key, jsonElement)
            }
        }
        logInfo(Tag, "Request body: $requestBody")
        return JsonHttpRequest().invoke(
            path = ConfigRequestPath,
            body = requestBody.toString().toByteArray(),
        ).map { jsonResponse ->
            val config = jsonResponse.getValue("init")
            keyValueStorage.token = jsonResponse.getValue("token").toString()
            BidonJson.decodeFromJsonElement(ConfigResponse.serializer(), config)
        }
    }
}

private const val ConfigRequestPath = "config"
private const val Tag = "ConfigRequestUseCase"
