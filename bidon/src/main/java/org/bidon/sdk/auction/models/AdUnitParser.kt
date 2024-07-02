package org.bidon.sdk.auction.models

import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.JsonParser
import org.json.JSONObject

// TODO: remove after discuss
private const val defaultTimeout = 5000L

internal class AdUnitParser : JsonParser<AdUnit> {
    override fun parseOrNull(jsonString: String): AdUnit? = runCatching {
        val json = JSONObject(jsonString)
        val extJson = json.optJSONObject("ext")
        AdUnit(
            uid = json.optString("uid", ""),
            demandId = json.optString("demand_id"),
            pricefloor = json.optDouble("pricefloor", 0.0),
            label = json.optString("label"),
            bidType = BidType.valueOf(json.optString("bid_type")),
            timeout = json.optLong("timeout", defaultTimeout),
            ext = extJson?.toString(),
        )
    }.getOrNull()
}