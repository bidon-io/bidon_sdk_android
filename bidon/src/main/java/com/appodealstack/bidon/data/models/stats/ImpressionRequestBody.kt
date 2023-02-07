package com.appodealstack.bidon.data.models.stats

import com.appodealstack.bidon.data.models.auction.BannerRequestBody
import com.appodealstack.bidon.data.models.auction.InterstitialRequestBody
import com.appodealstack.bidon.data.models.auction.RewardedRequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
@Serializable
internal data class ImpressionRequestBody(
    @SerialName("auction_id")
    val auctionId: String,
    @SerialName("auction_configuration_id")
    val auctionConfigurationId: Int,
    @SerialName("imp_id")
    val impressionId: String,
    @SerialName("demand_id")
    val demandId: String,
    @SerialName("ad_unit_id")
    val adUnitId: String?,
    @SerialName("ecpm")
    val ecpm: Double,
    @SerialName("banner")
    val banner: BannerRequestBody?,
    @SerialName("interstitial")
    val interstitial: InterstitialRequestBody?,
    @SerialName("rewarded")
    val rewarded: RewardedRequestBody?,
)