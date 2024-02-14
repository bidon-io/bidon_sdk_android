package org.bidon.sdk.auction.usecases

import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.BiddingResponse
import org.bidon.sdk.auction.models.TokenInfo

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
internal interface BidRequestUseCase {
    suspend fun invoke(
        adTypeParam: AdTypeParam,
        tokens: List<Pair<String, TokenInfo>>,
        extras: Map<String, Any>,
        bidfloor: Double,
        auctionId: String,
        roundId: String,
        auctionConfigurationUid: String?,
    ): Result<BiddingResponse>
}
