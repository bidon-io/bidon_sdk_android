package com.appodealstack.bidon.auctions.domain

import com.appodealstack.bidon.auctions.data.models.AuctionResult

interface AuctionResolver {
    suspend fun sortWinners(list: List<AuctionResult>): List<AuctionResult>
}