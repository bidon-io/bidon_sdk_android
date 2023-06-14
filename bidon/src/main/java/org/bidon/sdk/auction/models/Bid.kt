package org.bidon.sdk.auction.models

import org.bidon.sdk.utils.serializer.JsonName

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
internal data class Bid(
    @field:JsonName("id")
    val id: String,
    @field:JsonName("impid")
    val impressionId: String,
    @field:JsonName("demand_id")
    val demandId: String,
    @field:JsonName("payload")
    val payload: String,
    @field:JsonName("price")
    val price: Double,
    @field:JsonName("ext")
    val ext: Map<String, Any?>,
)