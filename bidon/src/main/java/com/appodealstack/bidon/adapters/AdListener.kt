package com.appodealstack.bidon.adapters

interface AdListener {

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

interface RewardedAdListener {
    fun onUserRewarded(ad: Ad, reward: Reward?) {}

}

data class Reward(
    val label: String,
    val amount: Int
)