package com.appodealstack.mads.auctions

import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.DemandAd
import com.appodealstack.mads.demands.ExtendedListener

internal interface AuctionListener {

    /**
     * Callback invokes after auction completed and winner is selected.
     * Calls immediately after [ExtendedListener.onWinnerFound]
     */
    fun auctionSucceed(demandAd: DemandAd, ad: AuctionData.Success)

    /**
     * Callback invokes after auction completed, but no winner found.
     */
    fun auctionFailed(demandAd: DemandAd, cause: Throwable)

    fun demandAuctionSucceed(demandAd: DemandAd, ad: AuctionData.Success) {}
    fun demandAuctionFailed(demandAd: DemandAd, ad: AuctionData.Failure) {}

    /**
     * Callback invokes after auction completed and winner is selected.
     * Exposes sorted list of loaded Ad.
     * Calls immediately before [AdListener.onAdLoaded]
     */
    fun winnerFound(demandAd: DemandAd, ads: List<AuctionData.Success>) {}
}