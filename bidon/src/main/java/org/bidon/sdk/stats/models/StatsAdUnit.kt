package org.bidon.sdk.stats.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable
import org.json.JSONObject

internal data class StatsAdUnit(
    @field:JsonName("demand_id")
    val demandId: String,
    @field:JsonName("status")
    val status: String?,
    @field:JsonName("price")
    val price: Double?,
    @field:JsonName("token_start_ts")
    val tokenStartTs: Long?,
    @field:JsonName("token_finish_ts")
    val tokenFinishTs: Long?,
    @field:JsonName("bid_type")
    val bidType: String?,
    @field:JsonName("fill_start_ts")
    val fillStartTs: Long?,
    @field:JsonName("fill_finish_ts")
    val fillFinishTs: Long?,
    @field:JsonName("ad_unit_uid")
    val adUnitUid: String?,
    @field:JsonName("ad_unit_label")
    val adUnitLabel: String?,
    @field:JsonName("error_message")
    val errorMessage: String? = null,
    val timeout: Long?,
    val ext: JSONObject?
) : Serializable