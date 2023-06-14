package org.bidon.sdk.auction.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 *
 * @param orientationCode is [AdObjectRequestBody.Orientation.code]
 */
internal data class BidRequestBody(
    @field:JsonName("auction_configuration_id")
    val auctionConfigurationId: Int?,
    @field:JsonName("id")
    val impressionId: String,
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
    val banner: BannerRequestBody?,
    @field:JsonName("interstitial")
    val interstitial: InterstitialRequestBody?,
    @field:JsonName("rewarded")
    val rewarded: RewardedRequestBody?,

) : Serializable {

    data class Token(
        @field:JsonName("token")
        val token: String
    ) : Serializable
}