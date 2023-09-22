package org.bidon.sdk.auction.usecases

import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.BiddingResponse

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
internal interface BidRequestUseCase {
    suspend fun invoke(
        adTypeParam: AdTypeParam,
        tokens: List<Pair<DemandId, String>>,
        extras: Map<String, Any>,
        bidfloor: Double,
        auctionId: String,
        roundId: String,
        auctionConfigurationId: Int?,
        auctionConfigurationUid: String?,
    ): Result<BiddingResponse>
}
