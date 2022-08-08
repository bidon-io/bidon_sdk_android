package com.appodealstack.bidon.auctions

interface AuctionResolver {
    suspend fun findWinner(list: List<AuctionResult>): AuctionResult?
}