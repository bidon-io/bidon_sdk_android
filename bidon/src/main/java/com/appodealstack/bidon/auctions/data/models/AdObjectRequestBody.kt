package com.appodealstack.bidon.auctions.data.models

import com.appodealstack.bidon.auctions.data.models.AdObjectRequestBody.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [orientationCode] is a [Orientation.code]
 *
 * [Interstitial.formatCodes] is a list of [Interstitial.Format.code]s
 *
 * [Banner.formatCode] is a [Banner.Format.code]
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
    val banner: Banner?,
    @SerialName("interstitial")
    val interstitial: Interstitial?,
    @SerialName("rewarded")
    val rewarded: Rewarded?,
) {
    @Serializable
    class Rewarded // rewarded has no parameters

    @Serializable
    class Interstitial // interstitial has no parameters

    @Serializable
    data class Banner(
        @SerialName("format")
        val formatCode: Int,
    ) {
        enum class Format(val code: Int) {
            Banner320x50(0),
            LeaderBoard728x90(1),
            MRec300x250(2),
            Banner320x50Adaptive(3),
        }
    }

    enum class Orientation(val code: Int) {
        Portrait(0),
        Landscape(1)
    }
}
