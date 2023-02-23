package org.bidon.bidmachine

import android.content.Context
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.ads.banner.BannerFormat

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
