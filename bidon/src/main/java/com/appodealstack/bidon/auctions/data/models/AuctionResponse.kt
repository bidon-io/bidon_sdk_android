package com.appodealstack.bidon.auctions.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class AuctionResponse(
    @SerialName("rounds")
    val rounds: List<Round>?,
    @SerialName("line_items")
    val lineItems: List<LineItem>?,
    @SerialName("min_price")
    val minPrice: Double?,
    @SerialName("token")
    val token: JsonObject?,
)