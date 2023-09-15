package org.bidon.sdk.auction.models

import org.bidon.sdk.utils.json.JsonParser
import org.json.JSONObject

/**
 * Created by Bidon Team on 06/02/2023.
 */
data class LineItem(
    val uid: String?,
    val demandId: String?,
    val pricefloor: Double = 0.0,
    val adUnitId: String?,
)

internal class LineItemParser : JsonParser<LineItem> {
    override fun parseOrNull(jsonString: String): LineItem? = runCatching {
        val json = JSONObject(jsonString)
        LineItem(
            uid = json.optString("uid", ""),
            demandId = json.optString("id"),
            pricefloor = json.optDouble("pricefloor", 0.0),
            adUnitId = json.optString("ad_unit_id")
        )
    }.getOrNull()
}