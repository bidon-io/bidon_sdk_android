package com.appodealstack.bidon.data.models.config

import com.appodealstack.bidon.data.json.JsonParser
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
data class ConfigResponse(
    val initializationTimeout: Long,
    val adapters: Map<String, JSONObject>
)

internal class ConfigResponseParser : JsonParser<ConfigResponse> {
    override fun parseOrNull(jsonString: String): ConfigResponse? = runCatching {
        val json = JSONObject(jsonString)
        ConfigResponse(
            initializationTimeout = json.getLong("tmax"),
            adapters = json.getJSONObject("adapters").let { jsonAdapters ->
                jsonAdapters.keys().asSequence().associateWith { adapterName ->
                    jsonAdapters.getJSONObject(adapterName)
                }
            }
        )
    }.getOrNull()
}
