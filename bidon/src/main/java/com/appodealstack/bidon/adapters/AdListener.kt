package com.appodealstack.bidon.adapters

@Deprecated("")
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
    fun onAdShown(ad: Ad)
    fun onAdShowFailed(cause: Throwable)
    fun onAdImpression(ad: Ad)
    fun onAdClicked(ad: Ad)
    fun onAdClosed(ad: Ad)
}

interface NewAdListener {

    /**
     * Callback invokes after auction completed and winner is selected.
     * Calls immediately after [ExtendedListener.onAuctionFinished]
     */
    fun onAdLoaded(ad: Ad)

    /**
     * Callback invokes after auction completed, but no winner found.
     */
    fun onAdLoadFailed(cause: Throwable)
    fun onAdShowFailed(cause: Throwable)
    fun onAdImpression(ad: Ad) // equals onAdShown
    fun onAdClicked(ad: Ad)
    fun onAdClosed(ad: Ad)
    fun onAdExpired(ad: Ad)
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