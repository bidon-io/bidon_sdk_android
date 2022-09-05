package com.appodealstack.bidon.config.data.models

import com.appodealstack.bidon.core.BidonJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * Map< AdapterName:String, AdapterInfo >
 */
@Serializable
internal data class ConfigRequestBody(
    @SerialName("adapters")
    val adapters: Map<String, AdapterInfo>
) {
    fun getJson() = BidonJson.encodeToString(this)
}
