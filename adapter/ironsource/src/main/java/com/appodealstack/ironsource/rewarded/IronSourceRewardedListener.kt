package com.appodealstack.ironsource.rewarded

import com.appodealstack.mads.demands.Ad
import com.appodealstack.mads.demands.RewardedAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo

interface IronSourceRewardedListener {
    fun onRewardedVideoAvailabilityChanged(available: Boolean)
    fun onRewardedVideoAdOpened()
    fun onRewardedVideoAdClosed()
    fun onRewardedVideoAdStarted()
    fun onRewardedVideoAdEnded()
    fun onRewardedVideoAdRewarded(reward: RewardedAdListener.Reward?)
    fun onRewardedVideoAdShowFailed(cause: Throwable)
    fun onRewardedVideoAdClicked()

    fun onDemandAdLoaded(ad: Ad) {}
    fun onDemandAdLoadFailed(cause: Throwable) {}
    fun onAuctionFinished(ads: List<Ad>) {}
}

/**
 * IS [AdInfo] object is presented in [Ad.sourceAd]
 *
 * val adInfo: AdInfo? = Ad.sourceAd as? AdInfo
 */
interface IronSourceLevelPlayRewardedListener {
    fun onAdReady(ad: Ad)
    fun onAdLoadFailed(cause: Throwable)

    fun onAdOpened(ad: Ad)
    fun onAdClicked(ad: Ad)
    fun onAdRewarded(ad: Ad, reward: RewardedAdListener.Reward?)
    fun onAdClosed(ad: Ad)
    fun onAdShowFailed(cause: Throwable)

    fun onDemandAdLoaded(ad: Ad) {}
    fun onDemandAdLoadFailed(cause: Throwable) {}
    fun onAuctionFinished(ads: List<Ad>) {}
}

