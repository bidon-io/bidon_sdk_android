package com.appodealstack.bidon.domain.auction.usecases

import com.appodealstack.bidon.data.models.auction.AuctionResponse
import com.appodealstack.bidon.data.models.config.AdapterInfo
import com.appodealstack.bidon.domain.auction.AdTypeParam
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface GetAuctionRequestUseCase {
    suspend fun request(
        placement: String,
        additionalData: AdTypeParam,
        auctionId: String,
        adapters: Map<String, AdapterInfo>
    ): Result<AuctionResponse>
}
