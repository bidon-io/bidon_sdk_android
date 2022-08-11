package com.appodealstack.bidon.auctions.domain.impl

import com.appodealstack.bidon.auctions.domain.AuctionResolver
import com.appodealstack.bidon.auctions.data.models.AuctionResult

internal val DefaultAuctionResolver: AuctionResolver by lazy {
    PriceAuctionResolver()
}

private class PriceAuctionResolver : AuctionResolver {
    override suspend fun sortWinners(list: List<AuctionResult>): List<AuctionResult> {
        return list.sortedByDescending {
            it.ad.price
        }
    }
}