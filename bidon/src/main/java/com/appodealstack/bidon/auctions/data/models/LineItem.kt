package com.appodealstack.bidon.auctions.data.models

import com.appodealstack.bidon.adapters.DemandId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LineItem(
    @SerialName("id")
    val demandId: String?,
    @SerialName("pricefloor")
    val priceFloor: Double = 0.0,
    @SerialName("ad_unit_id")
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