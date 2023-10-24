package org.bidon.sdk.auction.usecases

import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.ResultsCollector
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.AuctionResult
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
        roundIndex: Int,
        pricefloor: Double,
        adUnits: List<AdUnit>,
        resultsCollector: ResultsCollector,
        onFinish: (remainingLineItems: List<AdUnit>) -> Unit,
    ): Result<List<AuctionResult>>
}
