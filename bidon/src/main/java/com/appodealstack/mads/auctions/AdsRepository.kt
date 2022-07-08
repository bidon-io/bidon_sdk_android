package com.appodealstack.mads.auctions

import com.appodealstack.mads.demands.DemandAd

internal interface AdsRepository {
    fun clearResults(demandAd: DemandAd)
    fun getWinnerOrNull(demandAd: DemandAd, onWinnerFound: (AuctionResult?) -> Unit)

    fun saveAuction(demandAd: DemandAd, auction: Auction)
    fun isAuctionActive(demandAd: DemandAd): Boolean
    fun getResults(demandAd: DemandAd): List<AuctionResult>
}

internal class AdsRepositoryImpl : AdsRepository {
    private val auctionMap = mutableMapOf<DemandAd, Auction>()

    override fun clearResults(demandAd: DemandAd) {
        auctionMap.remove(demandAd)
    }

    override fun getWinnerOrNull(demandAd: DemandAd, onWinnerFound: (AuctionResult?) -> Unit) {
        auctionMap[demandAd]?.getWinnerOrNull(
            onWinnerFound = onWinnerFound
        )
    }

    override fun saveAuction(demandAd: DemandAd, auction: Auction) {
        auctionMap[demandAd] = auction
    }

    override fun isAuctionActive(demandAd: DemandAd): Boolean {
        return auctionMap[demandAd]?.isAuctionActive() ?: false
    }

    override fun getResults(demandAd: DemandAd): List<AuctionResult> {
        return auctionMap[demandAd]?.getResults() ?: emptyList()
    }
}