package org.bidon.sdk.auction

import org.bidon.sdk.auction.models.Round
import org.bidon.sdk.auction.usecases.models.BiddingResult

/**
 * Created by Aleksei Cherniaev on 26/07/2023.
 */
internal sealed interface RoundResult {
    object Idle : RoundResult

    class Results(
        val round: Round,
        val pricefloor: Double,
        val biddingResult: BiddingResult,
        val networkResults: List<AuctionResult>,
    ) : RoundResult
}