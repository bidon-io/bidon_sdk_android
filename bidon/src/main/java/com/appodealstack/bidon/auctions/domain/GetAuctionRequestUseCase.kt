package com.appodealstack.bidon.auctions.domain

import com.appodealstack.bidon.auctions.data.models.AuctionResponse

internal interface GetAuctionRequestUseCase {
    suspend fun request(): Result<AuctionResponse>
}
