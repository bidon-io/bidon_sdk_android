package com.appodealstack.bidon.auction.models

import com.appodealstack.bidon.auction.models.AdObjectRequestBody.*
import com.appodealstack.bidon.utils.json.JsonParsers
import com.appodealstack.bidon.utils.json.JsonSerializer
import com.appodealstack.bidon.utils.json.jsonObject
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 *
 * [orientationCode] is a [Orientation.code]
 * [BannerRequestBody.formatCode] is a [BannerRequestBody.Format.code]
 */
internal data class AdObjectRequestBody(
    val placementId: String,
    val orientationCode: String,
    val auctionId: String,
    val minPrice: Double,
    val banner: BannerRequestBody?,
    val interstitial: InterstitialRequestBody?,
    val rewarded: RewardedRequestBody?,
) {

    enum class Orientation(val code: String) {
        Portrait("PORTRAIT"),
        Landscape("LANDSCAPE")
    }
}

internal class AdObjectRequestBodySerializer : JsonSerializer<AdObjectRequestBody> {
    override fun serialize(data: AdObjectRequestBody): JSONObject =
        jsonObject {
            "placement_id" hasValue data.placementId
            "orientation" hasValue data.orientationCode
            "auction_id" hasValue data.auctionId
            "min_price" hasValue data.minPrice
            "banner" hasValue JsonParsers.serializeOrNull(data.banner)
            "interstitial" hasValue JsonParsers.serializeOrNull(data.interstitial)
            "rewarded" hasValue JsonParsers.serializeOrNull(data.rewarded)
        }
}
