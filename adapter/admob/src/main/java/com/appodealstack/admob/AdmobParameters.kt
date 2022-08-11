package com.appodealstack.admob

import android.view.ViewGroup
import com.appodealstack.bidon.adapters.AdSource
import com.appodealstack.bidon.adapters.AdapterParameters
import com.appodealstack.bidon.adapters.banners.BannerSize

object AdmobParameters : AdapterParameters

data class AdmobBannerParams(
    val admobLineItems: List<AdmobLineItem>,
    val bannerSize: BannerSize,
    val adContainer: ViewGroup?,
    val priceFloor: Double
) : AdSource.AdParams

data class AdmobLineItem(val price: Double, val adUnitId: String)


data class AdmobFullscreenAdParams(
    val admobLineItems: List<AdmobLineItem>,
    val priceFloor: Double
) : AdSource.AdParams
