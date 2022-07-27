package com.appodealstack.mads.auctions.impl

import com.appodealstack.mads.auctions.AuctionResolver
import com.appodealstack.mads.auctions.AuctionResolversHolder
import com.appodealstack.mads.demands.DemandAd

internal class AuctionResolversHolderImpl : AuctionResolversHolder {
    private val resolvers = mutableMapOf<DemandAd, AuctionResolver>()

    override fun setAuctionResolver(demandAd: DemandAd, auctionResolver: AuctionResolver) {
        resolvers[demandAd] = auctionResolver
    }

    override fun getAuctionResolver(demandAd: DemandAd): AuctionResolver {
        return resolvers[demandAd] ?: DefaultAuctionResolver
    }
}