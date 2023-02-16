package com.appodealstack.bidon.auction.models

import com.appodealstack.bidon.adapter.DemandId
import com.appodealstack.bidon.utils.json.JsonParser
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
data class LineItem(
    val demandId: String?,
    val priceFloor: Double = 0.0,
    val adUnitId: String?,
)

/**
 * Finding first [LineItem], which has the minimum LineItem.pricefloor, but greater then given [priceFloor].
 */
fun List<LineItem>.minByPricefloorOrNull(demandId: DemandId, priceFloor: Double): LineItem? {
    return this
        .filter { it.demandId == demandId.demandId }
        .filterNot { it.adUnitId.isNullOrBlank() }
        .sortedBy { it.priceFloor }
        .firstOrNull { it.priceFloor > priceFloor }
}

internal class LineItemParser : JsonParser<LineItem> {
    override fun parseOrNull(jsonString: String): LineItem? = runCatching {
        val json = JSONObject(jsonString)
        LineItem(
            demandId = json.optString("id"),
            priceFloor = json.optDouble("pricefloor", 0.0),
            adUnitId = json.optString("ad_unit_id")
        )
    }.getOrNull()
}