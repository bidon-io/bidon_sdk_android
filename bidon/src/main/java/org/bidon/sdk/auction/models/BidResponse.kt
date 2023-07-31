package org.bidon.sdk.auction.models

import org.bidon.sdk.utils.serializer.JsonName
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
internal data class BidResponse(
    @field:JsonName("id")
    val id: String,
    @field:JsonName("impid")
    val impressionId: String,
    @field:JsonName("price")
    val price: Double,
    @field:JsonName("demands")
    val demands: List<Pair<String, JSONObject>>
) {
    val demandId get() = demands.firstOrNull()?.first
    val json get() = demands.firstOrNull()?.second
}