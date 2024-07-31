package org.bidon.sdk.auction.usecases.models

import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.AuctionResult

/**
 * Created by Aleksei Cherniaev on 26/07/2023.
 */
internal sealed interface RoundResult {
    object Idle : RoundResult

    class Results(
        val pricefloor: Double,
        val biddingResult: BiddingResult,
        val networkResults: List<AuctionResult>,
        val noBidsInfo: List<AdUnit>?,
    ) : RoundResult {

        fun getAuctionResults(): List<AuctionResult> {
            return networkResults + (biddingResult as? BiddingResult.FilledAd)?.results.orEmpty()
        }
    }
}