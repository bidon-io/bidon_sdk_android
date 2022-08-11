package com.appodealstack.bidon.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Default Json - kotlinx.serialization encoder/decoder
 */
@OptIn(ExperimentalSerializationApi::class)
val BidonJson by lazy {
    Json {
        /**
         * Lets ignore unknown keys while parsing from Json
         */
        ignoreUnknownKeys = true

        /**
         * Sets Null-value if field is not presented in Json
         */
        explicitNulls = false

        /**
         * By default, special floating-point values like Double.NaN and infinities are not supported in JSON because the JSON specification prohibits it. You can enable their encoding using the allowSpecialFloatingPointValues property:
         * This example produces the following non-stardard JSON output, yet it is a widely used encoding for special values in JVM world:
         * {"value":NaN}
         */
        allowSpecialFloatingPointValues = true
    }
}


fun <T> JsonElement.parse(serializer: KSerializer<T>): T {
    return BidonJson.decodeFromJsonElement(
        deserializer = serializer,
        element = this
    )
}