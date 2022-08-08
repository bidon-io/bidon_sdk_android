package com.appodealstack.ironsource.interstitial

import com.appodealstack.bidon.demands.Ad
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo

interface IronSourceInterstitialListener {
    fun onInterstitialAdReady()
    fun onInterstitialAdLoadFailed(cause: Throwable)
    fun onInterstitialAdOpened()
    fun onInterstitialAdClosed()
    fun onInterstitialAdShowSucceeded()
    fun onInterstitialAdShowFailed(cause: Throwable)
    fun onInterstitialAdClicked()

    fun onDemandAdLoaded(ad: Ad) {}
    fun onDemandAdLoadFailed(cause: Throwable) {}
    fun onAuctionFinished(ads: List<Ad>) {}
}

/**
 * IS [AdInfo] object is presented in [Ad.sourceAd]
 *
 * val adInfo: AdInfo? = Ad.sourceAd as? AdInfo
 */
interface IronSourceLevelPlayInterstitialListener {
    fun onAdReady(ad: Ad)
    fun onAdLoadFailed(cause: Throwable)
    fun onAdOpened(ad: Ad)
    fun onAdShowSucceeded(ad: Ad)
    fun onAdShowFailed(cause: Throwable)
    fun onAdClicked(ad: Ad)
    fun onAdClosed(ad: Ad)

    fun onDemandAdLoaded(ad: Ad) {}
    fun onDemandAdLoadFailed(cause: Throwable) {}
    fun onAuctionFinished(ads: List<Ad>) {}
}

