package org.bidon.meta.impl

import android.app.Activity
import android.content.Context
import com.facebook.ads.AdSize
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo.isTablet
import org.bidon.sdk.auction.models.LineItem

class MetaFullscreenAuctionParams(
    val context: Context,
    val placementId: String,
    val payload: String,
    override val price: Double
) : AdAuctionParams {
    override val lineItem: LineItem? = null
}

class MetaBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    val placementId: String,
    val payload: String,
    override val price: Double
) : AdAuctionParams {

    override val lineItem: LineItem? = null

    val bannerSize: AdSize
        get() = when (bannerFormat) {
            BannerFormat.Banner -> AdSize.BANNER_320_50
            BannerFormat.LeaderBoard -> AdSize.BANNER_HEIGHT_90
            BannerFormat.MRec -> AdSize.RECTANGLE_HEIGHT_250
            BannerFormat.Adaptive -> if (isTablet) AdSize.BANNER_HEIGHT_90 else AdSize.BANNER_HEIGHT_50
        }
}