package org.bidon.sdk.auction.models

import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.JsonParser
import org.json.JSONObject

internal class AdUnitParser : JsonParser<AdUnit> {
    override fun parseOrNull(jsonString: String): AdUnit? = runCatching {
        val json = JSONObject(jsonString)
        AdUnit(
            uid = json.optString("uid", ""),
            demandId = json.optString("demand_id"),
            pricefloor = runCatching {
                json.getDouble("pricefloor")
            }.getOrNull(),
            label = json.optString("label"),
            bidType = BidType.valueOf(json.optString("bid_type")),
            ext = json.optJSONObject("ext")?.toString()
        )
    }.getOrNull()
}