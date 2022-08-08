package com.appodealstack.ironsource.banner

import com.appodealstack.bidon.demands.Ad

interface IronSourceLevelPlayBannerListener {
    fun onAdLoaded(ad: Ad)
    fun onAdLoadFailed(cause: Throwable)
    fun onAdClicked(ad: Ad)
    fun onAdLeftApplication(ad: Ad)
    fun onAdScreenPresented(ad: Ad)
    fun onAdScreenDismissed(ad: Ad)

    fun onDemandAdLoaded(ad: Ad) {}
    fun onDemandAdLoadFailed(cause: Throwable) {}
    fun onAuctionFinished(ads: List<Ad>) {}
}

interface IronSourceBannerListener {
    fun onBannerAdLoaded()
    fun onBannerAdLoadFailed(cause: Throwable)
    fun onBannerAdClicked()
    fun onBannerAdScreenPresented()
    fun onBannerAdScreenDismissed()
    fun onBannerAdLeftApplication()

    fun onDemandAdLoaded(ad: Ad) {}
    fun onDemandAdLoadFailed(cause: Throwable) {}
    fun onAuctionFinished(ads: List<Ad>) {}
}