package org.bidon.applovin

import android.app.Activity
import android.content.Context
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.LineItem

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
