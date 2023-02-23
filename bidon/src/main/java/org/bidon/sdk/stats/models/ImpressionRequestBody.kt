package org.bidon.sdk.stats.models

import org.bidon.sdk.auction.models.BannerRequestBody
import org.bidon.sdk.auction.models.InterstitialRequestBody
import org.bidon.sdk.auction.models.RewardedRequestBody
import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal data class ImpressionRequestBody(
    @field:JsonName("auction_id")
    val auctionId: String,
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
    val banner: BannerRequestBody?,
    @field:JsonName("interstitial")
    val interstitial: InterstitialRequestBody?,
    @field:JsonName("rewarded")
    val rewarded: RewardedRequestBody?,
): Serializable
