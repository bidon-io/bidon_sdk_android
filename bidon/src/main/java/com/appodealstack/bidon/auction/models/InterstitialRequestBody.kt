package com.appodealstack.bidon.auction.models

import com.appodealstack.bidon.utils.json.JsonSerializer
import com.appodealstack.bidon.utils.json.jsonObject
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class InterstitialRequestBody // interstitial has no parameters

internal class InterstitialRequestBodySerializer : JsonSerializer<InterstitialRequestBody> {
    override fun serialize(data: InterstitialRequestBody): JSONObject = jsonObject {}
}
