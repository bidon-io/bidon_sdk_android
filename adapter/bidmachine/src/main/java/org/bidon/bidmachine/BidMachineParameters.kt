package org.bidon.bidmachine

import android.app.Activity
import android.content.Context
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit

data class BidMachineParameters(
    val sellerId: String,
    val endpoint: String?,
    val mediationConfig: List<String>?,
) : AdapterParameters

class BMBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val price: Double,
    val timeout: Long,
    override val adUnit: AdUnit,
    val payload: String?
) : AdAuctionParams {

    override fun toString(): String {
        return "BMBannerAuctionParams(bannerFormat=$bannerFormat, pricefloor=$price, timeout=$timeout)"
    }
}

class BMFullscreenAuctionParams(
    val context: Context,
    override val price: Double,
    val timeout: Long,
    override val adUnit: AdUnit,
    val payload: String?
) : AdAuctionParams {

    override fun toString(): String {
        return "BMFullscreenAuctionParams(pricefloor=$price, timeout=$timeout)"
    }
}
