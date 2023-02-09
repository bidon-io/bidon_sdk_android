package com.appodealstack.bidon.data.models.config

import com.appodealstack.bidon.data.json.JsonSerializer
import com.appodealstack.bidon.data.json.jsonObject
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
data class AdapterInfo(
    val adapterVersion: String,
    val sdkVersion: String
)

internal class AdapterInfoSerializer : JsonSerializer<AdapterInfo> {
    override fun serialize(data: AdapterInfo): JSONObject =
        jsonObject {
            "version" hasValue data.adapterVersion
            "sdk_version" hasValue data.sdkVersion
        }
}