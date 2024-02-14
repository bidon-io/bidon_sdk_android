package org.bidon.vungle

import android.app.Activity
import com.vungle.ads.BannerAdSize
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo.isTablet
import org.bidon.sdk.auction.models.LineItem

class VungleBannerAuctionParams(
    val activity: Activity,
    override val price: Double,
    val bannerFormat: BannerFormat,
    val payload: String,
    val bannerId: String,
) : AdAuctionParams {
    override val lineItem: LineItem? = null
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
    override val price: Double,
    val placementId: String,
    val payload: String
) : AdAuctionParams {
    override val lineItem: LineItem? = null
}
