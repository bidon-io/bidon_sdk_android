package com.appodealstack.mads.auctions

import com.appodealstack.mads.demands.DemandAd

internal interface AuctionResolversHolder {
    fun setAuctionResolver(demandAd: DemandAd, auctionResolver: AuctionResolver)
    fun getAuctionResolver(demandAd: DemandAd): AuctionResolver
}