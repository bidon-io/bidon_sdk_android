package com.appodealstack.bidmachine

import android.content.Context
import com.appodealstack.bidon.adapter.AdAuctionParams
import com.appodealstack.bidon.adapter.AdapterParameters
import com.appodealstack.bidon.ads.banner.BannerFormat

data class BidMachineParameters(
    val sellerId: String,
    val endpoint: String?,
    val mediationConfig: List<String>?,
) : AdapterParameters

data class BMBannerAuctionParams(
    val context: Context,
    val bannerFormat: BannerFormat,
    val pricefloor: Double,
    val timeout: Long,
) : AdAuctionParams

data class BMFullscreenAuctionParams(
    val context: Context,
    val pricefloor: Double,
    val timeout: Long,
) : AdAuctionParams
