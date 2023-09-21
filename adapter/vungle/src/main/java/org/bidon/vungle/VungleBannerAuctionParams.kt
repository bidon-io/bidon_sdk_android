package org.bidon.vungle

import android.app.Activity
import com.vungle.warren.AdConfig
import com.vungle.warren.BannerAdConfig
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
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
            BannerFormat.LeaderBoard -> AdConfig.AdSize.BANNER_LEADERBOARD
            BannerFormat.MRec -> AdConfig.AdSize.VUNGLE_MREC
            BannerFormat.Adaptive -> AdConfig.AdSize.BANNER
            BannerFormat.Banner -> AdConfig.AdSize.BANNER
        }
    val config by lazy {
        BannerAdConfig().apply {
            this.adSize = bannerSize
        }
    }
}

class VungleFullscreenAuctionParams(
    override val price: Double,
    val placementId: String,
    val payload: String
) : AdAuctionParams {
    override val lineItem: LineItem? = null
}
