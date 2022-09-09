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
    val roundStatusCode: String,
    @SerialName("ecpm")
    val ecpm: Double?,
    @SerialName("bid_start_ts")
    val bidStartTs: Long?,
    @SerialName("bid_finish_ts")
    val bidFinishTs: Long?,
    @SerialName("fill_start_ts")
    val fillStartTs: Long?,
    @SerialName("fill_finish_ts")
    val fillFinishTs: Long?,
)
