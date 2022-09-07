package com.appodealstack.bidon.analytics.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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