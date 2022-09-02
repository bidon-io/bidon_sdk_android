package com.appodealstack.bidon.config.data.impl

import com.appodealstack.bidon.config.data.models.ConfigRequestBody
import com.appodealstack.bidon.config.data.models.ConfigResponse
import com.appodealstack.bidon.config.domain.DataBinderType
import com.appodealstack.bidon.config.domain.DataProvider
import com.appodealstack.bidon.config.domain.GetConfigRequestUseCase
import com.appodealstack.bidon.config.domain.databinders.CreateRequestBodyUseCase
import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.core.ext.logInfo
import com.appodealstack.bidon.utilities.keyvaluestorage.KeyValueStorage
import com.appodealstack.bidon.utilities.ktor.JsonHttpRequest

internal class GetConfigRequestUseCaseImpl(
    private val createRequestBody: CreateRequestBodyUseCase,
    private val keyValueStorage: KeyValueStorage
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
        val requestBody = createRequestBody(
            binders = binders,
            dataKeyName = "adapters",
            data = body,
            dataSerializer = ConfigRequestBody.serializer()
        )
        return JsonHttpRequest().invoke(
            path = ConfigRequestPath,
            body = requestBody,
        ).map { jsonResponse ->
            val config = jsonResponse.getValue("init")
            keyValueStorage.token = jsonResponse.getValue("token").toString()
            BidonJson.decodeFromJsonElement(ConfigResponse.serializer(), config)
        }
    }
}

private const val ConfigRequestPath = "config"
private const val Tag = "ConfigRequestUseCase"
