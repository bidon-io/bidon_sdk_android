package com.appodealstack.bidon.auctions.impl

import com.appodealstack.bidon.auctions.AuctionResolver
import com.appodealstack.bidon.auctions.AuctionResult

internal val DefaultAuctionResolver: AuctionResolver by lazy {
    PriceAuctionResolver()
}

private class PriceAuctionResolver : AuctionResolver {
    override suspend fun findWinner(list: List<AuctionResult>): AuctionResult? {
        return list.maxByOrNull {
            it.ad.price
        }
    }
}