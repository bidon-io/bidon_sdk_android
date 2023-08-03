package org.bidon.sdk.auction.models

import org.bidon.sdk.utils.json.JsonParser
import org.json.JSONObject

/**
 * Created by Bidon Team on 06/02/2023.
 */
data class RoundRequest(
    val id: String,
    val timeoutMs: Long,
    val demandIds: List<String>,
    val biddingIds: List<String>
)

internal class RoundParser : JsonParser<RoundRequest> {
    override fun parseOrNull(jsonString: String): RoundRequest? = runCatching {
        val json = JSONObject(jsonString)
        RoundRequest(
            id = json.getString("id"),
            timeoutMs = json.getLong("timeout"),
            demandIds = buildList {
                val jsonArray = json.optJSONArray("demands")
                if (jsonArray != null) {
                    repeat(jsonArray.length()) { index ->
                        add(jsonArray.getString(index))
                    }
                }
            },
            biddingIds = buildList {
                val jsonArray = json.optJSONArray("bidding")
                if (jsonArray != null) {
                    repeat(jsonArray.length()) { index ->
                        add(jsonArray.getString(index))
                    }
                }
            }
        )
    }.getOrNull()
}
