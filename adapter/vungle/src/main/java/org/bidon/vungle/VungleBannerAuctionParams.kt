package org.bidon.vungle

import android.app.Activity
import com.vungle.warren.AdConfig
import com.vungle.warren.BannerAdConfig
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
            BannerFormat.MRec -> AdConfig.AdSize.VUNGLE_MREC
            BannerFormat.LeaderBoard -> AdConfig.AdSize.BANNER_LEADERBOARD
            BannerFormat.Banner -> AdConfig.AdSize.BANNER
            BannerFormat.Adaptive -> if (isTablet) AdConfig.AdSize.BANNER_LEADERBOARD else AdConfig.AdSize.BANNER
        }
    val config by lazy {
        BannerAdConfig().apply {
            this.adSize = bannerSize
        }
    }
}

data class VungleFullscreenAuctionParams(
    override val price: Double,
    val placementId: String,
    val payload: String
) : AdAuctionParams {
    override val lineItem: LineItem? = null
}
