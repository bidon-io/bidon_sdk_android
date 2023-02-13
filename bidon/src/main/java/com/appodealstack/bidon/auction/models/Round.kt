package com.appodealstack.bidon.auction.models

import com.appodealstack.bidon.utils.json.JsonParser
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal data class Round(
    val id: String,
    val timeoutMs: Long,
    val demandIds: List<String>
)

internal class RoundParser : JsonParser<Round> {
    override fun parseOrNull(jsonString: String): Round? = runCatching {
        val json = JSONObject(jsonString)
        Round(
            id = json.getString("id"),
            timeoutMs = json.getLong("timeout"),
            demandIds = buildList {
                val jsonArray = json.getJSONArray("demands")
                repeat(jsonArray.length()) { index ->
                    add(jsonArray.getString(index))
                }
            }
        )
    }.getOrNull()
}
