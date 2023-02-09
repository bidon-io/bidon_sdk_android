package com.appodealstack.bidon.data.models.auction

import com.appodealstack.bidon.data.json.JsonSerializer
import com.appodealstack.bidon.data.json.jsonObject
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class RewardedRequestBody // rewarded has no parameters

internal class RewardedRequestBodySerializer : JsonSerializer<RewardedRequestBody> {
    override fun serialize(data: RewardedRequestBody): JSONObject = jsonObject {}
}
