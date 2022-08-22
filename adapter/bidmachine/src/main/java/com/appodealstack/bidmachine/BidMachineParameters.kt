package com.appodealstack.bidmachine

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
    val bannerSize: BannerSize,
    val priceFloor: Double,
) : AdAuctionParams

data class BMFullscreenAuctionParams(
    val priceFloor: Double,
    val timeout: Long,
) : AdAuctionParams
