package com.appodealstack.bidon.data.models.stats

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Demand(
    @SerialName("id")
    val demandId: String,
    @SerialName("ad_unit_id")
    val adUnitId: String?,
    @SerialName("status")
    val roundStatusCode: Int,
    @SerialName("ecpm")
    val ecpm: Double?,
    @SerialName("start_ts")
    val startTs: Long?,
    @SerialName("finish_ts")
    val finishTs: Long?,
)
