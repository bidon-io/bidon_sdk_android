package com.appodealstack.mads.auctions.impl

import com.appodealstack.mads.auctions.AuctionResolver
import com.appodealstack.mads.auctions.AuctionResult

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