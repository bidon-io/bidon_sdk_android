package org.bidon.sdk.auction.models

import org.bidon.sdk.utils.serializer.JsonName
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
data class BidResponse(
    @field:JsonName("id")
    val id: String,
    @field:JsonName("ad_unit")
    val adUnit: AdUnit,
    @field:JsonName("imp_id")
    val impressionId: String,
    @field:JsonName("price")
    val price: Double,
    @field:JsonName("ext")
    private val ext: String?
) {
    val extra get() = ext?.let { JSONObject(it) }
}