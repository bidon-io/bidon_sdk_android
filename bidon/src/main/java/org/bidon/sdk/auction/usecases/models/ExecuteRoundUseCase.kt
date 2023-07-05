package org.bidon.sdk.auction.usecases.models

import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.ResultsCollector
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.Round

/**
 * Created by Aleksei Cherniaev on 02/06/2023.
 */
internal interface ExecuteRoundUseCase {
    suspend operator fun invoke(
        demandAd: DemandAd,
        auctionResponse: AuctionResponse,
        adTypeParam: AdTypeParam,
        round: Round,
        pricefloor: Double,
        lineItems: List<LineItem>,
        resultsCollector: ResultsCollector,
        onFinish: (remainingLineItems: List<LineItem>) -> Unit,
    ): Result<List<AuctionResult>>
}
