package org.bidon.vungle

import android.app.Activity
import com.vungle.ads.BannerAdSize
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo.isTablet
import org.bidon.sdk.auction.models.AdUnit

class VungleBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val payload: String? = adUnit.extra?.getString("payload")
    val placementId: String? = adUnit.extra?.getString("placement_id")

    val bannerSize
        get() = when (bannerFormat) {
            BannerFormat.MRec -> BannerAdSize.VUNGLE_MREC
            BannerFormat.LeaderBoard -> BannerAdSize.BANNER_LEADERBOARD
            BannerFormat.Banner -> BannerAdSize.BANNER
            BannerFormat.Adaptive -> if (isTablet) BannerAdSize.BANNER_LEADERBOARD else BannerAdSize.BANNER
        }
}

class VungleFullscreenAuctionParams(
    val activity: Activity,
    override val adUnit: AdUnit
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val payload: String? = adUnit.extra?.getString("payload")
    val placementId: String? = adUnit.extra?.getString("placement_id")
}
