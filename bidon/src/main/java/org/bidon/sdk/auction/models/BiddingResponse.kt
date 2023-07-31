package org.bidon.sdk.auction.models

import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.utils.json.JsonParser
import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
internal data class BiddingResponse(
    @field:JsonName("bids")
    val bids: List<BidResponse>?,
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

internal class BidResponseParser : JsonParser<BiddingResponse> {
    override fun parseOrNull(jsonString: String): BiddingResponse? = runCatching {
        val json = JSONObject(jsonString)
        BiddingResponse(
            bids = json.optJSONArray("bids")?.let { array ->
                buildList {
                    repeat(array.length()) { index ->
                        array.optJSONObject(index)
                            ?.let { bidJson ->
                                val bid = BidResponse(
                                    id = bidJson.getString("id"),
                                    impressionId = bidJson.optString("impid"),
                                    price = bidJson.getDouble("price"),
                                    demands = bidJson.optJSONObject("demands")?.let { jsonObject ->
                                        jsonObject.keys().asSequence().mapNotNull { demandId ->
                                            jsonObject.optJSONObject(demandId)?.let { demandId to it }.also {
                                                if (it == null) {
                                                    logError(
                                                        TAG,
                                                        "DemandId($demandId) does not have JSONObject",
                                                        NullPointerException()
                                                    )
                                                }
                                            }
                                        }.toList()
                                    } ?: emptyList()
                                )
                                add(bid)
                            }
                    }
                }
            },
            status = json.getString("status").let {
                BiddingResponse.BidStatus.get(it)
            }
        )
    }.getOrNull()
}

private const val TAG = "BiddingResponse"