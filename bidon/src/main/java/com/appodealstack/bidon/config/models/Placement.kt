package com.appodealstack.bidon.config.models

import com.appodealstack.bidon.utils.json.JsonParsers
import com.appodealstack.bidon.utils.json.JsonSerializer
import com.appodealstack.bidon.utils.json.jsonObject
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
data class Placement(
    val name: String,
    val reward: Reward?,
    val capping: Capping?,
)

internal class PlacementSerializer : JsonSerializer<Placement> {
    override fun serialize(data: Placement): JSONObject {
        return jsonObject {
            "name" hasValue data.name
            "reward" hasValue JsonParsers.serializeOrNull(data.reward)
            "capping" hasValue JsonParsers.serializeOrNull(data.capping)
        }
    }
}
