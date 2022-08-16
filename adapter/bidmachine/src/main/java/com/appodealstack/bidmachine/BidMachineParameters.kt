package com.appodealstack.bidmachine

import com.appodealstack.bidon.adapters.AdSource
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
): AdapterParameters

data class BMBannerParams(
    val bannerSize: BannerSize,
    val priceFloor: Double,
): AdSource.AdParams

data class BMFullscreenParams(
    val priceFloor: Double,
    val timeout: Long,
): AdSource.AdParams