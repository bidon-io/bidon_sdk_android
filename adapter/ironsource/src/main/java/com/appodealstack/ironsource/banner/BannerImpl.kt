package com.appodealstack.ironsource.banner

import android.app.Activity
import com.appodealstack.ironsource.ISDecorator
import com.appodealstack.mads.demands.banners.BannerSize

internal class BannerImpl : ISDecorator.Banner {
    override fun createBanner(activity: Activity, bannerSize: BannerSize): BNIronSourceBannerLayout {
        return BNIronSourceBannerLayout(activity, null, bannerSize)
    }

    override fun startAutoRefresh(ironSourceBannerLayout: BNIronSourceBannerLayout) {
        ironSourceBannerLayout.startAutoRefresh()
    }

    override fun stopAutoRefresh(ironSourceBannerLayout: BNIronSourceBannerLayout) {
        ironSourceBannerLayout.stopAutoRefresh()
    }

    override fun setAutoRefreshTimeout(ironSourceBannerLayout: BNIronSourceBannerLayout, timeoutMs: Long) {
        ironSourceBannerLayout.setAutoRefreshTimeout(timeoutMs)
    }

    override fun loadBanner(ironSourceBannerLayout: BNIronSourceBannerLayout, placementName: String?) {
        ironSourceBannerLayout.loadAd(placementName)
    }

    override fun destroyBanner(ironSourceBannerLayout: BNIronSourceBannerLayout) {
        ironSourceBannerLayout.destroy()
    }
}