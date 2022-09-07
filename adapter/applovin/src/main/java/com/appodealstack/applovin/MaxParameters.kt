package com.appodealstack.applovin

import android.app.Activity
import android.content.Context
import com.appodealstack.bidon.data.models.auction.LineItem
import com.appodealstack.bidon.domain.adapter.AdAuctionParams
import com.appodealstack.bidon.domain.adapter.AdapterParameters
import com.appodealstack.bidon.domain.common.BannerSize
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
