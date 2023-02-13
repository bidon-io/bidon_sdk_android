package com.appodealstack.bidmachine

import android.content.Context
import com.appodealstack.bidon.adapter.AdAuctionParams
import com.appodealstack.bidon.adapter.AdapterParameters
import com.appodealstack.bidon.ads.banner.BannerSize

data class BidMachineParameters(
    val sellerId: String,
    val endpoint: String?,
    val mediationConfig: List<String>?,
) : AdapterParameters

data class BMBannerAuctionParams(
    val context: Context,
    val bannerSize: BannerSize,
    val priceFloor: Double,
    val timeout: Long,
) : AdAuctionParams

data class BMFullscreenAuctionParams(
    val context: Context,
    val priceFloor: Double,
    val timeout: Long,
) : AdAuctionParams
