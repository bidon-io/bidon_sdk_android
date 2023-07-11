package org.bidon.sdk.utils.json

import org.json.JSONObject

/**
 * Created by Bidon Team on 08/02/2023.
 */
interface JsonParser<T> {
    fun parseOrNull(jsonString: String): T?
}

interface JsonSerializer<T> {
    fun serialize(data: T): JSONObject
}