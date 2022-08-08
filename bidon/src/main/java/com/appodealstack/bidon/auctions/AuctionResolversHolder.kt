package com.appodealstack.bidon.auctions

import com.appodealstack.bidon.demands.DemandAd

internal interface AuctionResolversHolder {
    fun setAuctionResolver(demandAd: DemandAd, auctionResolver: AuctionResolver)
    fun getAuctionResolver(demandAd: DemandAd): AuctionResolver
}