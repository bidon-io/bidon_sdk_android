package com.appodealstack.applovin

import android.app.Activity
import android.content.Context
import com.appodealstack.bidon.adapter.AdAuctionParams
import com.appodealstack.bidon.adapter.AdapterParameters
import com.appodealstack.bidon.ads.banner.BannerFormat
import com.appodealstack.bidon.auction.models.LineItem

data class ApplovinParameters(
    val key: String,
) : AdapterParameters

data class ApplovinBannerAuctionParams(
    val context: Context,
    val bannerFormat: BannerFormat,
    val lineItem: LineItem,
    val adaptiveBannerHeight: Int?
) : AdAuctionParams

data class ApplovinFullscreenAdAuctionParams(
    val activity: Activity,
    val lineItem: LineItem,
    val timeoutMs: Long
) : AdAuctionParams
