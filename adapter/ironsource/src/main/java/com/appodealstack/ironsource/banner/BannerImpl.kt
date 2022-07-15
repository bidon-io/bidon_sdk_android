package com.appodealstack.ironsource.banner

import android.app.Activity
import com.appodealstack.ironsource.ISDecorator
import com.appodealstack.mads.demands.banners.BannerSize

internal class BannerImpl : ISDecorator.Banner {
    override fun createBanner(activity: Activity, bannerSize: BannerSize): BNIronSourceBannerLayout {
        return BNIronSourceBannerLayout(activity, null, bannerSize)
    }

    override fun loadBanner(ironSourceBannerLayout: BNIronSourceBannerLayout, placementName: String?) {
        ironSourceBannerLayout.loadAd(placementName)
    }

    override fun destroyBanner(ironSourceBannerLayout: BNIronSourceBannerLayout) {
        ironSourceBannerLayout.destroy()
    }
}