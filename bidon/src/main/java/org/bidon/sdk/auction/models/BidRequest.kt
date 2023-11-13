package org.bidon.sdk.auction.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 *
 * @param orientationCode is [AdObjectRequest.Orientation.code]
 */
internal data class BidRequest(
    @field:JsonName("auction_configuration_uid")
    val auctionConfigurationUid: String?,
    @field:JsonName("auction_id")
    val auctionId: String,
    @field:JsonName("round_id")
    val roundId: String,
    @field:JsonName("orientation")
    val orientationCode: String,
    @field:JsonName("bidfloor")
    val bidfloor: Double,
    @field:JsonName("demands")
    val demands: Map<String, Token>,
    @field:JsonName("banner")
    val banner: BannerRequest?,
    @field:JsonName("interstitial")
    val interstitial: InterstitialRequest?,
    @field:JsonName("rewarded")
    val rewarded: RewardedRequest?,

) : Serializable {

    data class Token(
        @field:JsonName("token")
        val token: String
    ) : Serializable
}