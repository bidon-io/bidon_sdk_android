package com.appodealstack.bidon.auctions

import com.appodealstack.bidon.adapters.DemandAd

internal interface AdsRepository {
    fun destroyResults(demandAd: DemandAd)
    fun getWinnerOrNull(demandAd: DemandAd, onWinnerFound: (AuctionResult?) -> Unit)

    fun saveAuction(demandAd: DemandAd, auction: Auction)
    fun isAuctionActive(demandAd: DemandAd): Boolean
    fun getResults(demandAd: DemandAd): List<AuctionResult>
}

internal class AdsRepositoryImpl : AdsRepository {
    private val auctionMap = mutableMapOf<DemandAd, Auction>()

    override fun destroyResults(demandAd: DemandAd) {
        auctionMap[demandAd]?.let { auction ->
            auction.getResults().forEach {
                it.adProvider.destroy()
            }
        }
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