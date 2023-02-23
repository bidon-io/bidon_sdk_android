package org.bidon.sdk.auction.models

import org.bidon.sdk.utils.json.JsonSerializer
import org.bidon.sdk.utils.json.jsonObject
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class InterstitialRequestBody // interstitial has no parameters

internal class InterstitialRequestBodySerializer : JsonSerializer<InterstitialRequestBody> {
    override fun serialize(data: InterstitialRequestBody): JSONObject = jsonObject {}
}
