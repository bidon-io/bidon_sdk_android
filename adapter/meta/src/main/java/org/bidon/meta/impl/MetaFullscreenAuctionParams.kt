package org.bidon.meta.impl

import android.content.Context
import com.facebook.ads.AdSize
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceType.isTablet

class MetaFullscreenAuctionParams(
    val context: Context,
    val placementId: String,
    val payload: String,
    override val price: Double
) : AdAuctionParams {
    override val adUnitId: String
        get() = placementId
}

class MetaBannerAuctionParams(
    val context: Context,
    val bannerFormat: BannerFormat,
    val placementId: String,
    val payload: String,
    override val price: Double
) : AdAuctionParams {
    override val adUnitId: String
        get() = placementId

    val bannerSize: AdSize
        get() = when (bannerFormat) {
            BannerFormat.Banner -> AdSize.BANNER_320_50
            BannerFormat.LeaderBoard -> AdSize.BANNER_HEIGHT_90
            BannerFormat.MRec -> AdSize.RECTANGLE_HEIGHT_250
            BannerFormat.Adaptive -> if (!isTablet) AdSize.BANNER_HEIGHT_50 else AdSize.BANNER_HEIGHT_90
        }
}