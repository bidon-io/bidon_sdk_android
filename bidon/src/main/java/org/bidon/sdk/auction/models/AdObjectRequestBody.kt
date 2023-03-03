package org.bidon.sdk.auction.models

import org.bidon.sdk.auction.models.AdObjectRequestBody.*
import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 *
 * [orientationCode] is a [Orientation.code]
 * [BannerRequestBody.formatCode] is a [BannerRequestBody.StatFormat.code]
 */
internal data class AdObjectRequestBody(
    @field:JsonName("placement_id")
    val placementId: String,
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
