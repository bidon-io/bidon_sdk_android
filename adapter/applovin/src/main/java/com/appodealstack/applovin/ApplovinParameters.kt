package com.appodealstack.applovin

import android.app.Activity
import com.appodealstack.bidon.adapters.AdAuctionParams
import com.appodealstack.bidon.adapters.AdapterParameters
import com.appodealstack.bidon.adapters.banners.BannerSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApplovinParameters(
    @SerialName("applovin_key")
    val key: String
) : AdapterParameters

data class ApplovinBannerAuctionParams(
    val bannerSize: BannerSize,
    val priceFloor: Double,
    val adUnitId: String,
    val adaptiveBannerHeight: Int?
) : AdAuctionParams

data class ApplovinFullscreenAdAuctionParams(
    val activity: Activity,
    val adUnitId: String,
    val timeoutMs: Long
) : AdAuctionParams
