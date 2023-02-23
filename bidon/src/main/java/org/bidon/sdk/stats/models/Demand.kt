package org.bidon.sdk.stats.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal data class Demand(
    @field:JsonName("id")
    val demandId: String,
    @field:JsonName("ad_unit_id")
    val adUnitId: String?,
    @field:JsonName("status")
    val roundStatusCode: String,
    @field:JsonName("ecpm")
    val ecpm: Double?,
    @field:JsonName("bid_start_ts")
    val bidStartTs: Long?,
    @field:JsonName("bid_finish_ts")
    val bidFinishTs: Long?,
    @field:JsonName("fill_start_ts")
    val fillStartTs: Long?,
    @field:JsonName("fill_finish_ts")
    val fillFinishTs: Long?,
): Serializable
