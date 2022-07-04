package com.appodealstack.mads.auctions

import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.DemandAd
import com.appodealstack.mads.demands.ExtendedListener

internal interface AuctionListener {

    /**
     * Callback invokes after auction completed and winner is selected.
     * Calls immediately after [ExtendedListener.onWinnerFound]
     */
    fun onAdLoaded(demandAd: DemandAd, ad: AuctionData.Success)

    /**
     * Callback invokes after auction completed, but no winner found.
     */
    fun onAdLoadFailed(demandAd: DemandAd, cause: Throwable)

    fun onDemandAdLoaded(demandAd: DemandAd, ad: AuctionData.Success) {}
    fun onDemandAdLoadFailed(demandAd: DemandAd, ad: AuctionData.Failure) {}

    /**
     * Callback invokes after auction completed and winner is selected.
     * Exposes sorted list of loaded Ad.
     * Calls immediately before [AdListener.onAdLoaded]
     */
    fun onWinnerFound(demandAd: DemandAd, ads: List<AuctionData.Success>) {}
}