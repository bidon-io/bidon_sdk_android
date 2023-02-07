package com.appodealstack.bidon.data.models.stats

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
@Serializable
data class Round(
    @SerialName("id")
    val id: String,
    @SerialName("pricefloor")
    val pricefloor: Double,
    @SerialName("winner_id")
    val winnerDemandId: String?,
    @SerialName("winner_ecpm")
    val winnerEcpm: Double?,
    @SerialName("demands")
    val demands: List<Demand>,
)