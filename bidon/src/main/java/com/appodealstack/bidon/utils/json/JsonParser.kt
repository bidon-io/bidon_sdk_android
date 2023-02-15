package com.appodealstack.bidon.utils.json

import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 08/02/2023.
 */
interface JsonParser<T> {
    fun parseOrNull(jsonString: String): T?
}

interface JsonSerializer<T> {
    fun serialize(data: T): JSONObject
}