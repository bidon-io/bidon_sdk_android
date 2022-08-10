package com.appodealstack.applovin.rewarded

import com.appodealstack.bidon.adapters.Ad
import com.appodealstack.bidon.adapters.RewardedAdListener

interface BNRewardedListener {
    fun onRewardedStarted(ad: Ad)
    fun onRewardedCompleted(ad: Ad)
    fun onUserRewarded(ad: Ad, reward: RewardedAdListener.Reward?)

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