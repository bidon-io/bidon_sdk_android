package org.bidon.sdk.auction.models

import org.bidon.sdk.auction.models.AdObjectRequest.Orientation
import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Bidon Team on 06/02/2023.
 *
 * @param [orientationCode] is a [Orientation.code]*
 */
internal data class AdObjectRequest(
    @field:JsonName("orientation")
    val orientationCode: String,
    @field:JsonName("auction_id")
    val auctionId: String,
    @field:JsonName("auction_key")
    val auctionKey: String?,
    @field:JsonName("auction_pricefloor")
    val pricefloor: Double,
    @field:JsonName("banner")
    val banner: BannerRequest?,
    @field:JsonName("interstitial")
    val interstitial: InterstitialRequest?,
    @field:JsonName("rewarded")
    val rewarded: RewardedRequest?,
    @field:JsonName("demands")
    val demands: Map<String, TokenInfo>,
) : Serializable {

    enum class Orientation(val code: String) {
        Portrait("PORTRAIT"),
        Landscape("LANDSCAPE")
    }
}
