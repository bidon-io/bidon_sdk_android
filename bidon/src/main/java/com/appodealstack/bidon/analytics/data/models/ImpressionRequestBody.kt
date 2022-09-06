package com.appodealstack.bidon.analytics.data.models

import com.appodealstack.bidon.auctions.data.models.BannerRequestBody
import com.appodealstack.bidon.auctions.data.models.InterstitialRequestBody
import com.appodealstack.bidon.auctions.data.models.RewardedRequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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