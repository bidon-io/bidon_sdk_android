package org.bidon.sdk.auction.usecases

import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.AuctionResponse
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
