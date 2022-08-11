package com.appodealstack.applovin

import com.appodealstack.bidon.adapters.AdSource
import com.appodealstack.bidon.adapters.AdapterParameters
import com.appodealstack.bidon.adapters.banners.BannerSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApplovinParameters(
    @SerialName("applovin_key")
    val key: String
): AdapterParameters

data class ApplovinBannerParams(
    val bannerSize: BannerSize,
    val priceFloor: Double,
    val adUnitId: String,
    val adaptiveBannerHeight: Int?
): AdSource.AdParams

data class ApplovinFullscreenAdParams(
    val adUnitId: String,
): AdSource.AdParams