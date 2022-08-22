package com.appodealstack.bidon.auctions.domain

import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.data.models.OldAuctionResult

@Deprecated("")
fun interface OldAuctionRequest {
    suspend fun execute(): Result<OldAuctionResult>
}

fun interface AuctionRequest {
    suspend fun execute(): Result<AuctionResult>
}
