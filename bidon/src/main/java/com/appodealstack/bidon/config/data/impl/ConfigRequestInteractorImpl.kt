package com.appodealstack.bidon.config.data.impl

import com.appodealstack.bidon.config.data.models.ConfigRequestBody
import com.appodealstack.bidon.config.data.models.ConfigResponse
import com.appodealstack.bidon.config.domain.ConfigRequestInteractor
import com.appodealstack.bidon.config.domain.DataProvider
import com.appodealstack.bidon.config.domain.DataBinderType
import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.utilities.network.AppodealEndpoints
import com.appodealstack.bidon.utilities.network.HttpClient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

internal class ConfigRequestInteractorImpl(
    private val dataProvider: DataProvider
) : ConfigRequestInteractor {
    private val BaseUrl = "https://herokuapp.appodeal.com/android_bidon_config"
//    private const val BaseUrl = "https://run.mocky.io/v3/a53f8ae1-f0c5-4e57-b25b-78f3831fb947"
//    private const val BaseUrl = "https://1e69e7f9-a8f2-4cc2-9d30-5a71dd5d6db2.mock.pstmn.io"

    private val httpClient: HttpClient = HttpClient.Zip
    private val binders: List<DataBinderType> = listOf(DataBinderType.Device)

    override suspend fun request(body: ConfigRequestBody): Result<ConfigResponse> {
        val bindData = dataProvider.provide(binders)
        val requestBody = buildJsonObject {
            put("adapters", BidonJson.encodeToJsonElement(body.adapters))
            bindData.forEach { (key, jsonElement) ->
                put(key, jsonElement)
            }
        }
        println(requestBody)
        return httpClient.enqueue(
            method = HttpClient.Method.POST,
            useUniqueRequestId = false,
            url = BaseUrl ?: "${AppodealEndpoints.activeEndpoint}/config",
            parser = { response ->
                requireNotNull(response) {
                    "Response is null /config"
                }
                BidonJson.decodeFromString<JsonObject>(String(response))
            },
            body = BidonJson.encodeToString(requestBody).toByteArray()
        ).mapCatching { jsonResponse ->
            requireNotNull(jsonResponse)
            val config = jsonResponse.getValue("init")
            BidonJson.decodeFromJsonElement(config)
        }
    }
}

