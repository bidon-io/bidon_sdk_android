package com.appodealstack.bidon.config.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ConfigResponse(
    @SerialName("tmax")
    val initializationTimeout: Long,
    @SerialName("adapters")
    val adapters: Map<String, JsonObject>
)