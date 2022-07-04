package com.appodealstack.mads.auctions

import com.appodealstack.mads.base.AdType
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.ExtendedListener

internal interface AuctionListener {

    /**
     * Callback invokes after auction completed and winner is selected.
     * Calls immediately after [ExtendedListener.onWinnerFound]
     */
    fun onAdLoaded(adType: AdType, ad: AuctionData.Success)

    /**
     * Callback invokes after auction completed, but no winner found.
     */
    fun onAdLoadFailed(adType: AdType, cause: Throwable)

    fun onDemandAdLoaded(adType: AdType, ad: AuctionData.Success) {}
    fun onDemandAdLoadFailed(adType: AdType, ad: AuctionData.Failure) {}

    /**
     * Callback invokes after auction completed and winner is selected.
     * Exposes sorted list of loaded Ad.
     * Calls immediately before [AdListener.onAdLoaded]
     */
    fun onWinnerFound(adType: AdType, ads: List<AuctionData.Success>) {}
}