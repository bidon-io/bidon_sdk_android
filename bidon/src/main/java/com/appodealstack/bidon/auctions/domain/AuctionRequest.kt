package com.appodealstack.bidon.auctions.domain

import com.appodealstack.bidon.auctions.data.models.AuctionResult

fun interface AuctionRequest {
    suspend fun execute(): Result<AuctionResult>
}

