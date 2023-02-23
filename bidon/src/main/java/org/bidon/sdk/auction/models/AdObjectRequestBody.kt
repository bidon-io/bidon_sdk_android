package org.bidon.sdk.auction.models

import org.bidon.sdk.auction.models.AdObjectRequestBody.*
import org.bidon.sdk.utils.json.JsonParsers
import org.bidon.sdk.utils.json.JsonSerializer
import org.bidon.sdk.utils.json.jsonObject
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
    val pricefloor: Double,
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
            "pricefloor" hasValue data.pricefloor
            "banner" hasValue JsonParsers.serializeOrNull(data.banner)
            "interstitial" hasValue JsonParsers.serializeOrNull(data.interstitial)
            "rewarded" hasValue JsonParsers.serializeOrNull(data.rewarded)
        }
}
