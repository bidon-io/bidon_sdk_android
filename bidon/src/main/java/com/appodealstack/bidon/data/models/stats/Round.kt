package com.appodealstack.bidon.data.models.stats

import com.appodealstack.bidon.data.json.JsonParsers
import com.appodealstack.bidon.data.json.JsonSerializer
import com.appodealstack.bidon.data.json.jsonArray
import com.appodealstack.bidon.data.json.jsonObject
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal data class Round(
    val id: String,
    val pricefloor: Double,
    val winnerDemandId: String?,
    val winnerEcpm: Double?,
    val demands: List<Demand>,
)

internal class RoundSerializer : JsonSerializer<Round> {
    override fun serialize(data: Round): JSONObject {
        return jsonObject {
            "id" hasValue data.id
            "pricefloor" hasValue data.pricefloor
            "winner_id" hasValue data.winnerDemandId
            "winner_ecpm" hasValue data.winnerEcpm
            "demands" hasValue jsonArray {
                val jsonObjects = data.demands.map {
                    JsonParsers.serialize(it)
                }
                putValues(jsonObjects)
            }
        }
    }
}