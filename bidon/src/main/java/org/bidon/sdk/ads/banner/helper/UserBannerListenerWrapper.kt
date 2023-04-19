package org.bidon.sdk.ads.banner.helper

import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.banner.BannerListener
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal fun wrapUserBannerListener(userListener: () -> BannerListener?) = object : BannerListener {
    override fun onAdLoaded(ad: Ad) {
        userListener()?.onAdLoaded(ad)
    }

    override fun onAdLoadFailed(cause: BidonError) {
        userListener()?.onAdLoadFailed(cause)
    }

    override fun onAdShown(ad: Ad) {
        userListener()?.onAdShown(ad)
    }

    override fun onAdClicked(ad: Ad) {
        userListener()?.onAdClicked(ad)
    }

    override fun onAdExpired(ad: Ad) {
        userListener()?.onAdExpired(ad)
    }

    override fun onRevenuePaid(ad: Ad, adValue: AdValue) {
        userListener()?.onRevenuePaid(ad, adValue)
    }
}