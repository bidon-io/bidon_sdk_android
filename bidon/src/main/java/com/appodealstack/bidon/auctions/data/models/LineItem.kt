package com.appodealstack.bidon.auctions.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LineItem(
    @SerialName("id")
    val demandId: String?,
    @SerialName("pricefloor")
    val priceFloor: Double?,
    @SerialName("ad_unit_id")
    val adUnitId: String?,
)