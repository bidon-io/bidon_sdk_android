package org.bidon.sdk.ads.banner.helper

import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AuctionInfo
import org.bidon.sdk.ads.banner.BannerListener
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal fun wrapUserBannerListener(userListener: () -> BannerListener?) = object : BannerListener {
    override fun onAdLoaded(ad: Ad, auctionInfo: AuctionInfo) {
        userListener()?.onAdLoaded(ad, auctionInfo)
    }

    override fun onAdLoadFailed(auctionInfo: AuctionInfo?, cause: BidonError) {
        userListener()?.onAdLoadFailed(auctionInfo, cause)
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

    override fun onAdShowFailed(cause: BidonError) {
        userListener()?.onAdShowFailed(cause)
    }
}