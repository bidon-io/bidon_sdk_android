package com.appodealstack.applovin

import android.app.Activity
import android.content.Context
import com.appodealstack.bidon.adapters.AdAuctionParams
import com.appodealstack.bidon.adapters.AdapterParameters
import com.appodealstack.bidon.adapters.banners.BannerSize
import com.appodealstack.bidon.auctions.data.models.LineItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MaxParameters(
    @SerialName("app_key")
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
