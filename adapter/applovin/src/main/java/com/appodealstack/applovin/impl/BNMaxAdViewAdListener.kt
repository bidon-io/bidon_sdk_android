package com.appodealstack.applovin.impl

import com.appodealstack.mads.demands.Ad
import com.appodealstack.mads.demands.AdListener

interface BNMaxAdViewAdListener {

    /**
     * This method will be invoked when the [com.applovin.mediation.ads.MaxAdView] has expanded full screen.
     *
     * @param ad An ad for which the ad view expanded for. Guaranteed not to be null.
     */
    fun onAdExpanded(ad: Ad)

    /**
     * This method will be invoked when the [com.applovin.mediation.ads.MaxAdView] has collapsed back to its original size.
     *
     * @param ad An ad for which the ad view collapsed for. Guaranteed not to be null.
     */
    fun onAdCollapsed(ad: Ad)

    fun onAdLoaded(ad: Ad) {}

    fun onAdLoadFailed(adUnitId: String?, error: Throwable) {}

    fun onAdDisplayFailed(error: Throwable) {}

    fun onAdClicked(ad: Ad) {}

    /**
     * Additional callbacks
     */
    fun onDemandAdLoaded(ad: Ad) {}
    fun onDemandAdLoadFailed(cause: Throwable) {}

    /**
     * Callback invokes after auction completed.
     * Calls immediately before [AdListener.onAdLoaded]
     */
    fun onAuctionFinished(ads: List<Ad>) {}
}