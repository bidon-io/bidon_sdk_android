package com.appodealstack.bidon.config.data.models

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
