package org.bidon.bidmachine

import android.app.Activity
import android.content.Context
import io.bidmachine.CustomParams
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.AdUnit

internal class BidMachineParameters(
    val sellerId: String,
    val endpoint: String?,
    val mediationConfig: List<String>?,
) : AdapterParameters

class BMBannerAuctionParams(
    override val price: Double,
    override val adUnit: AdUnit,
    val activity: Activity,
    val bannerFormat: BannerFormat,
    val timeout: Long,
    val customParameters: CustomParams,
    val payload: String?,
) : AdAuctionParams {

    override fun toString(): String {
        return "BMBannerAuctionParams(bannerFormat=$bannerFormat, pricefloor=$price, timeout=$timeout)"
    }
}

class BMFullscreenAuctionParams(
    override val price: Double,
    override val adUnit: AdUnit,
    val context: Context,
    val timeout: Long,
    val customParameters: CustomParams,
    val payload: String?,
) : AdAuctionParams {

    override fun toString(): String {
        return "BMFullscreenAuctionParams(pricefloor=$price, timeout=$timeout)"
    }
}
