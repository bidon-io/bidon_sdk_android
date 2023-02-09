package com.appodealstack.bidon.data.models.config

import com.appodealstack.bidon.data.json.JsonSerializer
import com.appodealstack.bidon.data.json.jsonObject
import org.json.JSONObject

// TODO clarify model
data class Capping(
    val setting: String,
    val value: Int,
)

internal class CappingSerializer : JsonSerializer<Capping> {
    override fun serialize(data: Capping): JSONObject {
        return jsonObject {
            "setting" hasValue data.setting
            "value" hasValue data.value
        }
    }
}