package com.appodealstack.bidon.config.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
data class ConfigResponse(
    @SerialName("tmax")
    val initializationTimeout: Long,
    @SerialName("adapters")
    val adapters: Map<String, JsonObject>
) {
    inline fun <reified T : AdapterInitializationInfo> getConfig(adapterName: String): T? {
        return adapters[adapterName]?.let { jsonObject ->
            Json.decodeFromJsonElement<T>(jsonObject)
        }
    }
}