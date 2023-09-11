package org.bidon.sdk.stats.models

import org.bidon.sdk.auction.models.BannerRequest
import org.bidon.sdk.auction.models.InterstitialRequest
import org.bidon.sdk.auction.models.RewardedRequest
import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal data class ImpressionRequestBody(
    @field:JsonName("auction_id")
    val auctionId: String,
    @field:JsonName("round_id")
    val roundId: String,
    @field:JsonName("round_idx")
    val roundIndex: Int,
    @field:JsonName("auction_configuration_id")
    val auctionConfigurationId: Int,
    @field:JsonName("imp_id")
    val impressionId: String,
    @field:JsonName("demand_id")
    val demandId: String,
    @field:JsonName("ad_unit_id")
    val adUnitId: String?,
    @field:JsonName("ecpm")
    val ecpm: Double,
    @field:JsonName("banner")
    val banner: BannerRequest?,
    @field:JsonName("interstitial")
    val interstitial: InterstitialRequest?,
    @field:JsonName("rewarded")
    val rewarded: RewardedRequest?,
) : Serializable
