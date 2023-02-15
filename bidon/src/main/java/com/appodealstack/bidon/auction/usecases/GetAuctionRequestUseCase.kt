package com.appodealstack.bidon.auction.usecases

import com.appodealstack.bidon.auction.AdTypeParam
import com.appodealstack.bidon.auction.models.AuctionResponse
import com.appodealstack.bidon.adapter.AdapterInfo
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface GetAuctionRequestUseCase {
    suspend fun request(
        placement: String,
        additionalData: AdTypeParam,
        auctionId: String,
        adapters: Map<String, AdapterInfo>,
    ): Result<AuctionResponse>
}
