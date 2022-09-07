package com.appodealstack.bidon.data.models.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Map< AdapterName:String, AdapterInfo >
 */
@Serializable
internal data class ConfigRequestBody(
    @SerialName("adapters")
    val adapters: Map<String, AdapterInfo>
)
