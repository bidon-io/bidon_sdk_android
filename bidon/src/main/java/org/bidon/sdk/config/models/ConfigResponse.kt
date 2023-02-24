package org.bidon.sdk.config.models

import org.bidon.sdk.utils.json.JsonParser
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal data class ConfigResponse(
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
