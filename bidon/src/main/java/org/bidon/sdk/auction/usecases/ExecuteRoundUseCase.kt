package org.bidon.sdk.auction.usecases

import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.ResultsCollector
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.RoundRequest

/**
 * Created by Aleksei Cherniaev on 02/06/2023.
 */
internal interface ExecuteRoundUseCase {
    suspend operator fun invoke(
        demandAd: DemandAd,
        auctionResponse: AuctionResponse,
        adTypeParam: AdTypeParam,
        round: RoundRequest,
        pricefloor: Double,
        lineItems: List<LineItem>,
        resultsCollector: ResultsCollector,
        onFinish: (remainingLineItems: List<LineItem>) -> Unit,
    ): Result<List<AuctionResult>>
}
