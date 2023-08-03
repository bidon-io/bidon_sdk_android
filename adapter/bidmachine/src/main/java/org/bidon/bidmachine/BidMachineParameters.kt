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

class BMBannerAuctionParams(
    val context: Context,
    val bannerFormat: BannerFormat,
    override val price: Double,
    val timeout: Long,
    val payload: String?,
) : AdAuctionParams {
    override val adUnitId: String? = null

    override fun toString(): String {
        return "BMBannerAuctionParams(bannerFormat=$bannerFormat, pricefloor=$price, timeout=$timeout)"
    }
}

class BMFullscreenAuctionParams(
    val context: Context,
    override val price: Double,
    val timeout: Long,
    val payload: String?,
) : AdAuctionParams {
    override val adUnitId: String? = null

    override fun toString(): String {
        return "BMFullscreenAuctionParams(pricefloor=$price, timeout=$timeout)"
    }
}
