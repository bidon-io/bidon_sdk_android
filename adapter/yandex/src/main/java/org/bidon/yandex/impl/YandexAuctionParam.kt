package org.bidon.yandex.impl

import android.app.Activity
import android.content.Context
import com.yandex.mobile.ads.banner.BannerAdSize
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo
import org.bidon.sdk.auction.models.AdUnit

internal class YandexBannerAuctionParam(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val adUnitId: String? = adUnit.extra?.optString("ad_unit_id")

    val bannerSize: BannerAdSize
        get() = when (bannerFormat) {
            BannerFormat.Banner -> BannerAdSize.fixedSize(activity, 320, 50)
            BannerFormat.LeaderBoard -> BannerAdSize.fixedSize(activity, 728, 90)
            BannerFormat.MRec -> BannerAdSize.fixedSize(activity, 300, 250)
            BannerFormat.Adaptive -> if (DeviceInfo.isTablet) {
                BannerAdSize.fixedSize(activity, 728, 90)
            } else {
                BannerAdSize.fixedSize(activity, 320, 50)
            }
        }
}

internal class YandexFullscreenAuctionParam(
    val context: Context,
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val adUnitId: String? = adUnit.extra?.optString("ad_unit_id")
}
