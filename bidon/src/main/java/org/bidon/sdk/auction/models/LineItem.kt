package org.bidon.sdk.auction.models

import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.utils.json.JsonParser
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
data class LineItem(
    val demandId: String?,
    val pricefloor: Double = 0.0,
    val adUnitId: String?,
)

/**
 * Finding first [LineItem], which has the minimum LineItem.pricefloor, but greater then given [pricefloor].
 */
fun List<LineItem>.minByPricefloorOrNull(demandId: DemandId, pricefloor: Double): LineItem? {
    return this
        .filter { it.demandId == demandId.demandId }
        .filterNot { it.adUnitId.isNullOrBlank() }
        .sortedBy { it.pricefloor }
        .firstOrNull { it.pricefloor > pricefloor }
}

internal class LineItemParser : JsonParser<LineItem> {
    override fun parseOrNull(jsonString: String): LineItem? = runCatching {
        val json = JSONObject(jsonString)
        LineItem(
            demandId = json.optString("id"),
            pricefloor = json.optDouble("pricefloor", 0.0),
            adUnitId = json.optString("ad_unit_id")
        )
    }.getOrNull()
}