package com.appodealstack.mads.demands

import com.appodealstack.mads.auctions.AuctionData

interface AdListener : ExtendedListener {

    /**
     * Callback invokes after auction completed and winner is selected.
     * Calls immediately after [ExtendedListener.onWinnerFound]
     */
    fun onAdLoaded(ad: AuctionData.Success)

    /**
     * Callback invokes after auction completed, but no winner found.
     */
    fun onAdLoadFailed(cause: Throwable)
    fun onAdDisplayed(ad: AuctionData.Success)
    fun onAdDisplayFailed(ad: AuctionData.Failure)
    fun onAdClicked(ad: AuctionData.Success)
    fun onAdHidden(ad: AuctionData.Success)
}

interface ExtendedListener {
    fun onDemandAdLoaded(ad: AuctionData.Success) {}
    fun onDemandAdLoadFailed(ad: AuctionData.Failure) {}

    /**
     * Callback invokes after auction completed and winner is selected.
     * Exposes sorted list of loaded Ad.
     * Calls immediately before [AdListener.onAdLoaded]
     */
    fun onWinnerFound(ads: List<AuctionData.Success>) {}
}

