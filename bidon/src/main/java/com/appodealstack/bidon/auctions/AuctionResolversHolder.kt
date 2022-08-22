package com.appodealstack.bidon.auctions

import com.appodealstack.bidon.adapters.DemandAd
import com.appodealstack.bidon.auctions.domain.AuctionResolver

internal interface AuctionResolversHolder {
    fun setAuctionResolver(demandAd: DemandAd, auctionResolver: AuctionResolver)
    fun getAuctionResolver(demandAd: DemandAd): AuctionResolver
}
