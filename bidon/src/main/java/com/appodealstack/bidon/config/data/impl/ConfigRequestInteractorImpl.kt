package com.appodealstack.bidon.config.data.impl

import com.appodealstack.bidon.config.data.models.ConfigRequestBody
import com.appodealstack.bidon.config.data.models.ConfigResponse
import com.appodealstack.bidon.config.domain.ConfigRequestInteractor
import com.appodealstack.bidon.config.domain.DataBinderType
import com.appodealstack.bidon.config.domain.DataProvider
import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.core.ext.logInfo
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement

internal class ConfigRequestInteractorImpl(
    private val dataProvider: DataProvider,
    private val httpClient: HttpClient
) : ConfigRequestInteractor {
    private val BaseUrl = "65fd4c26-cf49-4905-901d-b251c4c3f2ab.mock.pstmn.io"
//    private val BaseUrl = "https://herokuapp.appodeal.com/android_bidon_config"
//    private const val BaseUrl = "https://run.mocky.io/v3/a53f8ae1-f0c5-4e57-b25b-78f3831fb947"
//    private const val BaseUrl = "https://1e69e7f9-a8f2-4cc2-9d30-5a71dd5d6db2.mock.pstmn.io"

    //    private val httpClient: HttpClient = HttpClient.Zip
    private val binders: List<DataBinderType> = listOf(DataBinderType.Device)

    override suspend fun request(body: ConfigRequestBody): Result<ConfigResponse> = runCatching {
        val bindData = dataProvider.provide(binders)
        val requestBody = buildJsonObject {
            put("adapters", BidonJson.encodeToJsonElement(body.adapters))
            bindData.forEach { (key, jsonElement) ->
                put(key, jsonElement)
            }
        }
        logInfo(Tag, "Request body: $requestBody")
        val response = httpClient.post {
            contentType(ContentType.Application.Json)
            url {
                protocol = URLProtocol.HTTPS
                host = BaseUrl
                path("/config")
            }
            setBody(requestBody)
        }
        val jsonResponse = response.body<JsonObject>()
        val config = jsonResponse.getValue("init")
        BidonJson.decodeFromJsonElement(ConfigResponse.serializer(), config)
    }
}

private const val Tag = "ConfigRequestInteractor"
