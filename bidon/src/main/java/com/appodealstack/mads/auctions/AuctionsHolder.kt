package com.appodealstack.mads.auctions

import com.appodealstack.mads.demands.DemandAd

internal interface AuctionsHolder {
    fun clearResults(demandAd: DemandAd)
    fun getTopResultOrNull(demandAd: DemandAd): AuctionData.Success?

    fun addAuction(demandAd: DemandAd, auction: Auction)
    fun isAuctionActive(demandAd: DemandAd): Boolean
}

internal class AuctionsHolderImpl : AuctionsHolder {
    private val auctionMap = mutableMapOf<DemandAd, Auction>()

    override fun clearResults(demandAd: DemandAd) {
        auctionMap.remove(demandAd)
    }

    override fun getTopResultOrNull(demandAd: DemandAd): AuctionData.Success? {
        return auctionMap[demandAd]?.getTopResultOrNull()
    }

    override fun addAuction(demandAd: DemandAd, auction: Auction) {
        auctionMap[demandAd] = auction
    }

    override fun isAuctionActive(demandAd: DemandAd): Boolean {
        return auctionMap[demandAd]?.isAuctionActive() ?: false
    }
}