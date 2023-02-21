package com.appodealstack.admob

import android.content.Context
import android.view.ViewGroup
import com.appodealstack.bidon.adapter.AdAuctionParams
import com.appodealstack.bidon.adapter.AdapterParameters
import com.appodealstack.bidon.ads.banner.BannerFormat
import com.appodealstack.bidon.auction.models.LineItem

object AdmobInitParameters : AdapterParameters

data class AdmobBannerAuctionParams(
    val adContainer: ViewGroup,
    val bannerFormat: BannerFormat,
    val lineItem: LineItem,
    val pricefloor: Double
) : AdAuctionParams

data class AdmobFullscreenAdAuctionParams(
    val context: Context,
    val lineItem: LineItem,
    val pricefloor: Double
) : AdAuctionParams
