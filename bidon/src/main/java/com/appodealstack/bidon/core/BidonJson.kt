package com.appodealstack.bidon.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Default Json - kotlinx.serialization encoder/decoder
 *
 * @param [ignoreUnknownKeys] lets ignore unknown keys while parsing from Json
 * @param [explicitNulls] sets Null-value if field is not presented in Json
 */
@OptIn(ExperimentalSerializationApi::class)
val BidonJson by lazy {
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
}


fun <T> JsonElement.parse(serializer: KSerializer<T>): T {
    return BidonJson.decodeFromJsonElement(
        deserializer = serializer,
        element = this
    )
}