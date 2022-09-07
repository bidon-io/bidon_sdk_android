package com.appodealstack.bidon.domain.auction

internal interface AuctionResolver {
    suspend fun sortWinners(list: List<AuctionResult>): List<AuctionResult>
}
