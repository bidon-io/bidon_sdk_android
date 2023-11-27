package org.bidon.sdk.auction.models

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
                        runCatching {
                            array.optJSONObject(index)
                                ?.let { bidJson ->
                                    val adUnitJson = bidJson.getJSONObject("ad_unit").toString()
                                    val adUnit = requireNotNull(AdUnitParser().parseOrNull(adUnitJson)) {
                                        "AdUnit is null for bid $bidJson"
                                    }
                                    val bid = BidResponse(
                                        id = bidJson.getString("id"),
                                        impressionId = bidJson.optString("imp_id"),
                                        price = bidJson.getDouble("price"),
                                        adUnit = adUnit,
                                        ext = bidJson.optJSONObject("ext")?.toString()
                                    )
                                    add(bid)
                                }
                        }.onFailure {
                            println("Failed to parse bid $index: ${it.message}")
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