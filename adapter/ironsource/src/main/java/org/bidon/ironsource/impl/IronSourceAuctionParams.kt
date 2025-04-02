package org.bidon.ironsource.impl

import android.app.Activity
import com.ironsource.mediationsdk.ISBannerSize
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo.isTablet
import org.bidon.sdk.auction.models.AdUnit

internal class IronSourceFullscreenAuctionParams(
    val activity: Activity,
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val instanceId: String? = adUnit.extra?.getString("instance_id")
}

internal class IronSourceBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val instanceId: String? = adUnit.extra?.getString("instance_id")

    val bannerSize: ISBannerSize
        get() = when (bannerFormat) {
            BannerFormat.MRec -> ISBannerSize.RECTANGLE
            BannerFormat.LeaderBoard -> ISBannerSize.LARGE
            BannerFormat.Banner -> ISBannerSize.BANNER
            BannerFormat.Adaptive -> if (isTablet) ISBannerSize.LARGE else ISBannerSize.BANNER
        }
}
