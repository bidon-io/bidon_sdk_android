package com.appodealstack.mads.auctions

interface AuctionResolver {
    suspend fun findWinner(list: List<AuctionResult>): AuctionResult?
}