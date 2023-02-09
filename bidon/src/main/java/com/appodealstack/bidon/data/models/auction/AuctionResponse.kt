package com.appodealstack.bidon.data.models.auction

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
@Serializable
internal data class AuctionResponse(
    @SerialName("rounds")
    val rounds: List<Round>?,
    @SerialName("line_items")
    val lineItems: List<LineItem>?,
    @SerialName("min_price")
    val minPrice: Double?,
    @SerialName("fill_timeout")
    val fillTimeout: Long?,
    @SerialName("token")
    val token: String?,
    @SerialName("auction_id")
    val auctionId: String?,
    @SerialName("auction_configuration_id")
    val auctionConfigurationId: Int?,
)