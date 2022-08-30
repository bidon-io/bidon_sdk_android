package com.appodealstack.admob

import android.content.Context
import android.view.ViewGroup
import com.appodealstack.bidon.adapters.AdAuctionParams
import com.appodealstack.bidon.adapters.AdapterParameters
import com.appodealstack.bidon.adapters.banners.BannerSize
import com.appodealstack.bidon.auctions.data.models.LineItem

object AdmobInitParameters : AdapterParameters

data class AdmobBannerAuctionParams(
    val adContainer: ViewGroup,
    val bannerSize: BannerSize,
    val lineItem: LineItem,
) : AdAuctionParams

data class AdmobFullscreenAdAuctionParams(
    val context: Context,
    val lineItem: LineItem,
    val priceFloor: Double
) : AdAuctionParams
