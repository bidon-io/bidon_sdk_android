package com.appodealstack.bidon.config.domain

import com.appodealstack.bidon.core.BidonJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
            BidonJson.decodeFromJsonElement<T>(jsonObject)
        }
    }
}