package org.bidon.chartboost.impl

import android.app.Activity
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.ads.Banner
import org.bidon.chartboost.ext.adapterVersion
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo
import org.bidon.sdk.auction.models.AdUnit

internal class ChartboostFullscreenAuctionParams(
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val adLocation: String = adUnit.extra?.getString("ad_location") ?: "Default"
    val mediation get() = BidonMediator
}

internal class ChartboostBannerAuctionParams(
    val activity: Activity,
    val bannerFormat: BannerFormat,
    override val adUnit: AdUnit,
) : AdAuctionParams {
    override val price: Double = adUnit.pricefloor
    val adLocation: String = adUnit.extra?.getString("ad_location") ?: "Default"
    val mediation get() = BidonMediator

    val bannerSize: Banner.BannerSize
        get() = when (bannerFormat) {
            BannerFormat.Banner -> Banner.BannerSize.STANDARD
            BannerFormat.LeaderBoard -> Banner.BannerSize.LEADERBOARD
            BannerFormat.MRec -> Banner.BannerSize.MEDIUM
            BannerFormat.Adaptive -> if (DeviceInfo.isTablet) Banner.BannerSize.LEADERBOARD else Banner.BannerSize.STANDARD
        }
}

private val BidonMediator by lazy {
    Mediation(
        /* mediationType = */ "Bidon",
        /* libraryVersion = */ BidonSdk.SdkVersion,
        /* adapterVersion = */ adapterVersion
    )
}