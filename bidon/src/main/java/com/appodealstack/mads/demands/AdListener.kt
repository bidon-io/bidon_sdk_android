package com.appodealstack.mads.demands

interface AdListener : ExtendedListener, RewardedAdListener {

    /**
     * Callback invokes after auction completed and winner is selected.
     * Calls immediately after [ExtendedListener.onAuctionFinished]
     */
    fun onAdLoaded(ad: Ad)

    /**
     * Callback invokes after auction completed, but no winner found.
     */
    fun onAdLoadFailed(cause: Throwable)
    fun onAdDisplayed(ad: Ad)
    fun onAdDisplayFailed(cause: Throwable)
    fun onAdImpression(ad: Ad)
    fun onAdClicked(ad: Ad)
    fun onAdHidden(ad: Ad)
}

interface ExtendedListener {
    fun onDemandAdLoaded(ad: Ad) {}
    fun onDemandAdLoadFailed(cause: Throwable) {}

    /**
     * Callback invokes after auction completed.
     * Calls immediately before [AdListener.onAdLoaded]
     */
    fun onAuctionFinished(ads: List<Ad>) {}
}

interface RewardedAdListener {
    fun onRewardedStarted(ad: Ad) {}
    fun onRewardedCompleted(ad: Ad) {}
    fun onUserRewarded(ad: Ad, reward: Reward?) {}

    data class Reward(
        val label: String,
        val amount: Int
    )
}

