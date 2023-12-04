package org.bidon.sdk.auction.usecases

import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.AuctionResponse
/**
 * Created by Bidon Team on 06/02/2023.
 */
internal interface GetAuctionRequestUseCase {
    suspend fun request(
        adTypeParam: AdTypeParam,
        auctionId: String,
        demandAd: DemandAd,
        adapters: Map<String, AdapterInfo>,
    ): Result<AuctionResponse>
}
