package com.appodealstack.bidon.auctions.domain.impl

import com.appodealstack.bidon.auctions.domain.AuctionResolver
import com.appodealstack.bidon.auctions.AuctionResolversHolder
import com.appodealstack.bidon.adapters.DemandAd

internal class AuctionResolversHolderImpl : AuctionResolversHolder {
    private val resolvers = mutableMapOf<DemandAd, AuctionResolver>()

    override fun setAuctionResolver(demandAd: DemandAd, auctionResolver: AuctionResolver) {
        resolvers[demandAd] = auctionResolver
    }

    override fun getAuctionResolver(demandAd: DemandAd): AuctionResolver {
        return resolvers[demandAd] ?: DefaultAuctionResolver
    }
}