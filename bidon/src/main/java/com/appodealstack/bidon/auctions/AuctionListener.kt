package com.appodealstack.bidon.auctions

import com.appodealstack.bidon.adapters.AdListener
import com.appodealstack.bidon.adapters.DemandAd
import com.appodealstack.bidon.adapters.ExtendedListener
import com.appodealstack.bidon.auctions.data.models.OldAuctionResult

@Deprecated("")
internal interface AuctionListener {

    /**
     * Callback invokes after auction completed, but no winner found.
     */
    fun auctionFailed(demandAd: DemandAd, cause: Throwable)

    fun demandAuctionSucceed(auctionResult: OldAuctionResult) {}

    fun demandAuctionFailed(demandAd: DemandAd, error: Throwable) {}
    /**
     * Callback invokes after auction completed and winner is selected.
     * Exposes sorted list of loaded Ad.
     * Calls immediately before [AdListener.onAdLoaded]
     */
    fun auctionSucceed(demandAd: DemandAd, results: List<OldAuctionResult>) {}

    /**
     * Callback invokes after auction completed and winner is selected.
     * Calls immediately after [ExtendedListener.onAuctionFinished]
     */
    fun winnerFound(winner: OldAuctionResult)
}