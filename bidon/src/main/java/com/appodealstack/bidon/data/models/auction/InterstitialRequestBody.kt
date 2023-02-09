package com.appodealstack.bidon.data.models.auction

import com.appodealstack.bidon.data.json.JsonSerializer
import com.appodealstack.bidon.data.json.jsonObject
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class InterstitialRequestBody // interstitial has no parameters

internal class InterstitialRequestBodySerializer : JsonSerializer<InterstitialRequestBody> {
    override fun serialize(data: InterstitialRequestBody): JSONObject = jsonObject {}
}
