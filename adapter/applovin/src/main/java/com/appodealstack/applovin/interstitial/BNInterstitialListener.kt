package com.appodealstack.applovin.interstitial

import com.appodealstack.bidon.adapters.Ad

interface BNInterstitialListener {

    /**
     * Callback invokes after auction completed and winner is selected.
     * Calls immediately after [onAuctionFinished]
     */
    fun onAdLoaded(ad: Ad)

    /**
     * Callback invokes after auction completed, but no winner found.
     */
    fun onAdLoadFailed(cause: Throwable)
    fun onAdDisplayed(ad: Ad)
    fun onAdDisplayFailed(cause: Throwable)
    fun onAdClicked(ad: Ad)
    fun onAdHidden(ad: Ad)

    /**
     * Additional callbacks
     */
    fun onDemandAdLoaded(ad: Ad) {}
    fun onDemandAdLoadFailed(cause: Throwable) {}

    /**
     * Callback invokes after auction completed.
     * Calls immediately before [onAdLoaded]
     */
    fun onAuctionFinished(ads: List<Ad>) {}
}