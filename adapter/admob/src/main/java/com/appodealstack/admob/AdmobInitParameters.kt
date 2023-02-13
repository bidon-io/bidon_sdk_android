package com.appodealstack.admob

import android.content.Context
import android.view.ViewGroup
import com.appodealstack.bidon.adapter.AdAuctionParams
import com.appodealstack.bidon.adapter.AdapterParameters
import com.appodealstack.bidon.ads.banner.BannerSize
import com.appodealstack.bidon.auction.models.LineItem

object AdmobInitParameters : AdapterParameters

data class AdmobBannerAuctionParams(
    val adContainer: ViewGroup,
    val bannerSize: BannerSize,
    val lineItem: LineItem,
    val priceFloor: Double
) : AdAuctionParams

data class AdmobFullscreenAdAuctionParams(
    val context: Context,
    val lineItem: LineItem,
    val priceFloor: Double
) : AdAuctionParams
