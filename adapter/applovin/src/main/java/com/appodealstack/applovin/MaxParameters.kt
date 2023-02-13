package com.appodealstack.applovin

import android.app.Activity
import android.content.Context
import com.appodealstack.bidon.adapter.AdAuctionParams
import com.appodealstack.bidon.adapter.AdapterParameters
import com.appodealstack.bidon.ads.banner.BannerSize
import com.appodealstack.bidon.auction.models.LineItem

data class MaxParameters(
    val key: String
) : AdapterParameters

data class MaxBannerAuctionParams(
    val context: Context,
    val bannerSize: BannerSize,
    val lineItem: LineItem,
    val adaptiveBannerHeight: Int?
) : AdAuctionParams

data class MaxFullscreenAdAuctionParams(
    val activity: Activity,
    val lineItem: LineItem,
    val timeoutMs: Long
) : AdAuctionParams
