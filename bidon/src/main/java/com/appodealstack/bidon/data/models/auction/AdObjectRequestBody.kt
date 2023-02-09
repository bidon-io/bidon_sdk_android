package com.appodealstack.bidon.data.models.auction

import com.appodealstack.bidon.data.json.JsonParsers
import com.appodealstack.bidon.data.json.JsonSerializer
import com.appodealstack.bidon.data.json.jsonObject
import com.appodealstack.bidon.data.models.auction.AdObjectRequestBody.*
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 *
 * [orientationCode] is a [Orientation.code]
 * [InterstitialRequestBody.formatCodes] is a list of [InterstitialRequestBody.Format.code]s
 * [BannerRequestBody.formatCode] is a [BannerRequestBody.Format.code]
 */
internal data class AdObjectRequestBody(
    val placementId: String,
    val orientationCode: Int,
    val auctionId: String,
    val minPrice: Double,
    val banner: BannerRequestBody?,
    val interstitial: InterstitialRequestBody?,
    val rewarded: RewardedRequestBody?,
) {

    enum class Orientation(val code: Int) {
        Portrait(0),
        Landscape(1)
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
