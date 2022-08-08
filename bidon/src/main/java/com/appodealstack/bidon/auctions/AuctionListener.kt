package com.appodealstack.bidon.auctions

import com.appodealstack.bidon.demands.AdListener
import com.appodealstack.bidon.demands.DemandAd
import com.appodealstack.bidon.demands.ExtendedListener

internal interface AuctionListener {

    /**
     * Callback invokes after auction completed, but no winner found.
     */
    fun auctionFailed(demandAd: DemandAd, cause: Throwable)

    fun demandAuctionSucceed(auctionResult: AuctionResult) {}

    fun demandAuctionFailed(demandAd: DemandAd, error: Throwable) {}
    /**
     * Callback invokes after auction completed and winner is selected.
     * Exposes sorted list of loaded Ad.
     * Calls immediately before [AdListener.onAdLoaded]
     */
    fun auctionSucceed(demandAd: DemandAd, results: List<AuctionResult>) {}

    /**
     * Callback invokes after auction completed and winner is selected.
     * Calls immediately after [ExtendedListener.onAuctionFinished]
     */
    fun winnerFound(winner: AuctionResult)
}