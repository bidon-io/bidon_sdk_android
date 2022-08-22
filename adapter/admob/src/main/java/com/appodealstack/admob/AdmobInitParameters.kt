package com.appodealstack.admob

import android.content.Context
import android.view.ViewGroup
import com.appodealstack.bidon.adapters.AdAuctionParams
import com.appodealstack.bidon.adapters.AdapterParameters
import com.appodealstack.bidon.adapters.banners.BannerSize

object AdmobInitParameters : AdapterParameters

data class AdmobBannerAuctionParams(
    val admobLineItems: List<AdmobLineItem>,
    val bannerSize: BannerSize,
    val adContainer: ViewGroup,
    val priceFloor: Double
) : AdAuctionParams

data class AdmobLineItem(val price: Double, val adUnitId: String)

data class AdmobFullscreenAdAuctionParams(
    val context: Context,
    val admobLineItems: List<AdmobLineItem>,
    val priceFloor: Double
) : AdAuctionParams
