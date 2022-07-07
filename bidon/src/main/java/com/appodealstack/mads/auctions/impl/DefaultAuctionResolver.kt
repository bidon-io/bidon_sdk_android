package com.appodealstack.mads.auctions.impl

import com.appodealstack.mads.auctions.AuctionResolver
import com.appodealstack.mads.auctions.AuctionResult

internal class DefaultAuctionResolver : AuctionResolver{
    override suspend fun findWinner(list: List<AuctionResult>): AuctionResult? {
        return list.maxByOrNull {
            it.ad.price
        }
    }
}