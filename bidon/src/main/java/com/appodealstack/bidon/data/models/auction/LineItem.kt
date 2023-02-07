package com.appodealstack.bidon.data.models.auction

import com.appodealstack.bidon.domain.common.DemandId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
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