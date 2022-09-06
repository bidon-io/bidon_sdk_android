package com.appodealstack.bidon.auctions.data.models

import com.appodealstack.bidon.auctions.data.models.AdObjectRequestBody.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [orientationCode] is a [Orientation.code]
 *
 * [InterstitialRequestBody.formatCodes] is a list of [InterstitialRequestBody.Format.code]s
 *
 * [BannerRequestBody.formatCode] is a [BannerRequestBody.Format.code]
 */

@Serializable
internal data class AdObjectRequestBody(
    @SerialName("placement_id")
    val placementId: String,
    @SerialName("orientation")
    val orientationCode: Int,
    @SerialName("auction_id")
    val auctionId: String,
    @SerialName("banner")
    val banner: BannerRequestBody?,
    @SerialName("interstitial")
    val interstitial: InterstitialRequestBody?,
    @SerialName("rewarded")
    val rewarded: RewardedRequestBody?,
) {

    enum class Orientation(val code: Int) {
        Portrait(0),
        Landscape(1)
    }
}

