package com.appodealstack.ironsource

import com.appodealstack.bidon.adapters.AdSource
import com.appodealstack.bidon.adapters.AdapterParameters
import com.appodealstack.bidon.adapters.banners.BannerSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IronSourceParameters(
    @SerialName("app_key")
    val appKey: String,
) : AdapterParameters

data class ISBannerParams(
    val bannerSize: BannerSize,
): AdSource.AdParams
