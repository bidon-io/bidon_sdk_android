package com.appodealstack.mads.auctions

import com.appodealstack.mads.demands.Ad
import com.appodealstack.mads.demands.AdProvider

fun interface AuctionRequest {
    suspend fun execute(data: Data?): Result<AuctionResult>

    data class Data(val priceFloor: Double)
}

class AuctionResult(
    val ad: Ad,
    val adProvider: AdProvider
)