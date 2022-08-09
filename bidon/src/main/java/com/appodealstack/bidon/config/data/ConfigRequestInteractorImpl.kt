package com.appodealstack.bidon.config.data

import com.appodealstack.bidon.config.domain.ConfigRequestBody
import com.appodealstack.bidon.config.domain.ConfigRequestInteractor
import com.appodealstack.bidon.config.domain.ConfigResponse
import com.appodealstack.bidon.utilities.network.AppodealEndpoints
import com.appodealstack.bidon.utilities.network.HttpClient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

internal class ConfigRequestInteractorImpl : ConfigRequestInteractor {
    private val httpClient: HttpClient = HttpClient.Zip

    private val binders: List<DataBinder> = listOf()

    override suspend fun request(body: ConfigRequestBody): Result<ConfigResponse> {
        val jsonObject = buildJsonObject {
            put("adapters", Json.encodeToJsonElement(body))
            binders.forEach { dataBinder ->
                put(dataBinder.fieldName, dataBinder.getJsonElement())
            }
        }
        return httpClient.enqueue(
            method = HttpClient.Method.POST,
            useUniqueRequestId = false,
            url = "${AppodealEndpoints.activeEndpoint}/config",
            parser = { response ->
                requireNotNull(response) {
                    "Response is null /config"
                }
                Json.decodeFromString<JsonObject>(String(response))
            },
            body = Json.encodeToString(jsonObject).toByteArray()
        ).mapCatching { jsonResponse ->
            requireNotNull(jsonResponse)
            val config = jsonResponse.getValue("init")
            Json.decodeFromJsonElement(config)
        }
    }
}

interface DataBinder {
    val fieldName: String
    fun getJsonElement(): JsonElement
}