package com.appodealstack.bidon.data.models.stats

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
@Serializable
internal data class StatsRequestBody(
    @SerialName("auction_id")
    val auctionId: String,
    @SerialName("auction_configuration_id")
    val auctionConfigurationId: Int,
    @SerialName("rounds")
    val rounds: List<Round>,
)
