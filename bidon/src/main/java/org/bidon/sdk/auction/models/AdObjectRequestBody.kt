package org.bidon.sdk.auction.models

import org.bidon.sdk.auction.models.AdObjectRequestBody.*
import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Bidon Team on 06/02/2023.
 *
 * @param [orientationCode] is a [Orientation.code]*
 */
internal data class AdObjectRequestBody(
    @field:JsonName("orientation")
    val orientationCode: String,
    @field:JsonName("auction_id")
    val auctionId: String,
    @field:JsonName("pricefloor")
    val pricefloor: Double,
    @field:JsonName("banner")
    val banner: BannerRequestBody?,
    @field:JsonName("interstitial")
    val interstitial: InterstitialRequestBody?,
    @field:JsonName("rewarded")
    val rewarded: RewardedRequestBody?,
) : Serializable {

    enum class Orientation(val code: String) {
        Portrait("PORTRAIT"),
        Landscape("LANDSCAPE")
    }
}
