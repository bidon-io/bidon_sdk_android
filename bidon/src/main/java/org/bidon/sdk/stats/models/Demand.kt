package org.bidon.sdk.stats.models

import org.bidon.sdk.utils.json.JsonSerializer
import org.bidon.sdk.utils.json.jsonObject
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal data class Demand(
    val demandId: String,
    val adUnitId: String?,
    val roundStatusCode: String,
    val ecpm: Double?,
    val bidStartTs: Long?,
    val bidFinishTs: Long?,
    val fillStartTs: Long?,
    val fillFinishTs: Long?,
)

internal class DemandSerializer : JsonSerializer<Demand> {
    override fun serialize(data: Demand): JSONObject {
        return jsonObject {
            "id" hasValue data.demandId
            "ad_unit_id" hasValue data.adUnitId
            "status" hasValue data.roundStatusCode
            "ecpm" hasValue data.ecpm
            "bid_start_ts" hasValue data.bidStartTs
            "bid_finish_ts" hasValue data.bidFinishTs
            "fill_start_ts" hasValue data.fillStartTs
            "fill_finish_ts" hasValue data.fillFinishTs
        }
    }
}
