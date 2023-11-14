package org.bidon.sdk.stats.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal sealed interface DemandStat {

    data class Network(
        @field:JsonName("demand_id")
        val demandId: String,
        @field:JsonName("ad_unit_uid")
        val adUnitUid: String?,
        @field:JsonName("ad_unit_label")
        val adUnitLabel: String?,
        @field:JsonName("status")
        val roundStatusCode: String,
        @field:JsonName("price")
        val price: Double?,
        @field:JsonName("fill_start_ts")
        val fillStartTs: Long?,
        @field:JsonName("fill_finish_ts")
        val fillFinishTs: Long?,
    ) : Serializable, DemandStat

    data class Bidding(
        @field:JsonName("bid_start_ts")
        val bidStartTs: Long?,
        @field:JsonName("bid_finish_ts")
        val bidFinishTs: Long?,
        @field:JsonName("bids")
        val bids: List<Bid>,
    ) : Serializable, DemandStat {

        data class Bid(
            @field:JsonName("status")
            val roundStatusCode: String,
            @field:JsonName("demand_id")
            val demandId: String?,
            @field:JsonName("ad_unit_uid")
            val adUnitUid: String?,
            @field:JsonName("ad_unit_label")
            val adUnitLabel: String?,
            @field:JsonName("price")
            val price: Double?,
            @field:JsonName("token_start_ts")
            val tokenStartTs: Long?,
            @field:JsonName("token_finish_ts")
            val tokenFinishTs: Long?,
            @field:JsonName("fill_start_ts")
            val fillStartTs: Long?,
            @field:JsonName("fill_finish_ts")
            val fillFinishTs: Long?,
        ) : Serializable
    }
}