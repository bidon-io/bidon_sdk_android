package org.bidon.startio.impl

import android.app.Activity
import android.content.Context
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo
import org.bidon.sdk.auction.models.AdUnit

internal class StartIoFullscreenAuctionParams(
    val context: Context,
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val tag: String? = adUnit.extra?.optString("tag_id")
    val payload: String? = adUnit.extra?.optString("payload")
}

internal class StartIoBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val payload: String? = adUnit.extra?.optString("payload")
    val tag: String? = adUnit.extra?.optString("tag_id")
    val bannerSize: Pair<Int, Int>
        get() = when (bannerFormat) {
            BannerFormat.Banner -> 320 to 50
            BannerFormat.LeaderBoard -> 728 to 90
            BannerFormat.MRec -> 300 to 250
            BannerFormat.Adaptive -> if (DeviceInfo.isTablet) 728 to 90 else 320 to 50
        }
}
