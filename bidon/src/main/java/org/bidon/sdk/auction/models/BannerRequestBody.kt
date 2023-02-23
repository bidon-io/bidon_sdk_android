package org.bidon.sdk.auction.models

import org.bidon.sdk.utils.json.JsonSerializer
import org.bidon.sdk.utils.json.jsonObject
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
data class BannerRequestBody(
    val formatCode: String,
) {
    enum class Format(val code: String) {
        Banner320x50("BANNER"),
        LeaderBoard728x90("LEADERBOARD"),
        MRec300x250("MREC"),
        AdaptiveBanner320x50("ADAPTIVE"),
    }
}

internal class BannerRequestBodySerializer : JsonSerializer<BannerRequestBody> {
    override fun serialize(data: BannerRequestBody): JSONObject =
        jsonObject {
            "format" hasValue data.formatCode
        }
}