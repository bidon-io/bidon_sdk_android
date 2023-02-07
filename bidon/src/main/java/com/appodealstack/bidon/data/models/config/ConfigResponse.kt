package com.appodealstack.bidon.data.models.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
@Serializable
data class ConfigResponse(
    @SerialName("tmax")
    val initializationTimeout: Long,
    @SerialName("adapters")
    val adapters: Map<String, JsonObject>
)
