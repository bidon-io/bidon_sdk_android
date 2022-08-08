package com.appodeal.mads

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.appodealstack.ironsource.IronSourceDecorator
import com.appodealstack.ironsource.banner.IronSourceLevelPlayBannerListener
import com.appodealstack.bidon.demands.Ad
import com.appodealstack.bidon.demands.banners.BannerSize

class MainActivity : FragmentActivity() {
    private val bannerListener = object : IronSourceLevelPlayBannerListener {
        override fun onAdLoaded(ad: Ad) {
            /**
             * Invoked when there is a change in the ad availability status.
             */
        }

        override fun onAdLoadFailed(cause: Throwable) {}

        override fun onAdClicked(ad: Ad) {
            /**
             * Invoked when the end user clicked on the RewardedVideo ad
             */
        }
        override fun onAdLeftApplication(ad: Ad) {}
        override fun onAdScreenPresented(ad: Ad) {}
        override fun onAdScreenDismissed(ad: Ad) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bannerViewLayout = IronSourceDecorator.createBanner(this, BannerSize.Banner)
        bannerViewLayout.setLevelPlayBannerListener(bannerListener)

        IronSourceDecorator.loadBanner(bannerViewLayout)
    }
}