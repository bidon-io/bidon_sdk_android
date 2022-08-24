package com.appodealstack.bidmachine

import android.content.Context
import com.appodealstack.bidon.adapters.AdAuctionParams
import com.appodealstack.bidon.adapters.AdapterParameters
import com.appodealstack.bidon.adapters.banners.BannerSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BidMachineParameters(
    @SerialName("seller_id")
    val sellerId: String,
    @SerialName("endpoint")
    val endpoint: String?,
    @SerialName("mediation_config")
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
