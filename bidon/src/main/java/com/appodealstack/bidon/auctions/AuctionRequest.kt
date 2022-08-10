package com.appodealstack.bidon.auctions

import com.appodealstack.bidon.adapters.Ad
import com.appodealstack.bidon.adapters.AdProvider

fun interface AuctionRequest {
    suspend fun execute(data: Data?): Result<AuctionResult>

    data class Data(val priceFloor: Double)
}

class AuctionResult(
    val ad: Ad,
    val adProvider: AdProvider
) {
    override fun toString(): String {
        return "AuctionResult($ad)"
    }
}