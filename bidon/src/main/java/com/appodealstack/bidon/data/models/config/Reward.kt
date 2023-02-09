package com.appodealstack.bidon.data.models.config

import com.appodealstack.bidon.data.json.JsonSerializer
import com.appodealstack.bidon.data.json.jsonObject
import org.json.JSONObject

data class Reward(
    val currency: String,
    val amount: Int,
)

internal class RewardSerializer : JsonSerializer<Reward> {
    override fun serialize(data: Reward): JSONObject {
        return jsonObject {
            "title" hasValue data.currency
            "value" hasValue data.amount
        }
    }
}