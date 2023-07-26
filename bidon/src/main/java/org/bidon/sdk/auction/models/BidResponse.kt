package org.bidon.sdk.auction.models

import org.bidon.sdk.utils.json.JsonParser
import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
internal data class BidResponse(
    @field:JsonName("bid")
    val bid: Bid?,
    @field:JsonName("status")
    val status: BidStatus,
) : Serializable {
    enum class BidStatus(val code: String) {
        Success("SUCCESS"),
        NoBid("NO_BID");

        companion object {
            fun get(code: String) = values().first { it.code == code }
        }
    }
}

internal class BidResponseParser : JsonParser<BidResponse> {
    override fun parseOrNull(jsonString: String): BidResponse? = runCatching {
        val json = JSONObject(jsonString)
        BidResponse(
            bid = json.optJSONObject("bid")?.let { bidJson ->
                Bid(
                    id = bidJson.getString("id"),
                    impressionId = bidJson.optString("impid"),
                    payload = bidJson.getString("payload"),
                    demandId = bidJson.optString("demand_id"),
                    price = bidJson.getDouble("price"),
                    ext = bidJson.optJSONObject("ext")?.let { extJson ->
                        buildMap<String, Any> {
                            extJson.keys().forEach { key ->
                                extJson.optJSONObject(key)?.let { put(key, it) }
                            }
                        }
                    } ?: emptyMap()
                )
            },
            status = json.getString("status").let {
                BidResponse.BidStatus.get(it)
            }
        )
    }.getOrNull()
}