package com.appodealstack.bidon.stats.models

import com.appodealstack.bidon.utils.json.JsonParsers
import com.appodealstack.bidon.utils.json.JsonSerializer
import com.appodealstack.bidon.utils.json.jsonArray
import com.appodealstack.bidon.utils.json.jsonObject
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal data class StatsRequestBody(
    val auctionId: String,
    val auctionConfigurationId: Int,
    val rounds: List<Round>,
)

internal class StatsRequestBodySerializer : JsonSerializer<StatsRequestBody> {
    override fun serialize(data: StatsRequestBody): JSONObject {
        return jsonObject {
            "auction_id" hasValue data.auctionId
            "auction_configuration_id" hasValue data.auctionConfigurationId
            "rounds" hasValue jsonArray {
                val jsonObjects = data.rounds.map { round ->
                    JsonParsers.serialize(round)
                }
                putValues(jsonObjects)
            }
        }
    }
}
