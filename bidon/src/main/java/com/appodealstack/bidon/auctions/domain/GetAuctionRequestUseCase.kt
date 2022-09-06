package com.appodealstack.bidon.auctions.domain

import com.appodealstack.bidon.auctions.data.models.AdTypeAdditional
import com.appodealstack.bidon.auctions.data.models.AuctionResponse
import com.appodealstack.bidon.config.data.models.AdapterInfo

internal interface GetAuctionRequestUseCase {
    suspend fun request(
        placement: String,
        additionalData: AdTypeAdditional,
        auctionId: String,
        adapters: Map<String, AdapterInfo>
    ): Result<AuctionResponse>
}
