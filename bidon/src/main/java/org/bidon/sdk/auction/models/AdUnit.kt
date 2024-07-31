package org.bidon.sdk.auction.models

import org.bidon.sdk.stats.models.BidType
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 24/10/2023.
 */
data class AdUnit(
    val demandId: String,
    val label: String,
    val pricefloor: Double,
    val uid: String,
    val bidType: BidType,
    val timeout: Long,
    private val ext: String?,
) {
    val extra: JSONObject? = ext?.let {
        JSONObject(it)
    }

    override fun toString() =
        "Demand: $demandId, Label: $label Pricefloor: $pricefloor, UID: $uid, BidType: $bidType, timeout: $timeout"
}