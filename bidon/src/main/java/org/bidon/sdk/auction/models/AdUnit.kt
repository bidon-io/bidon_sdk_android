package org.bidon.sdk.auction.models

import org.bidon.sdk.utils.json.JsonParser
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 24/10/2023.
 */
data class AdUnit(
    val demandId: String,
    val label: String,
    val pricefloor: Double?,
    val uid: String,
    private val ext: String?,
) {
    val extra: JSONObject? = ext?.let {
        JSONObject(it)
    }
}

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
            ext = json.optJSONObject("ext")?.toString()
        )
    }.getOrNull()
}