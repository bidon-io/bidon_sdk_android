package org.bidon.sdk.auction

import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.auction.models.BidResponse
import org.bidon.sdk.auction.models.RoundRequest
import org.bidon.sdk.auction.usecases.models.RoundResult

/**
 * Created by Aleksei Cherniaev on 05/07/2023.
 */
internal interface ResultsCollector {
    fun startRound(round: RoundRequest, pricefloor: Double)
    fun serverBiddingStarted()
    fun serverBiddingFinished(bids: List<BidResponse>?)
    fun add(result: AuctionResult)
    fun getRoundResults(): RoundResult

    fun getAll(): List<AuctionResult>
    fun clear()
    suspend fun saveWinners(sourcePriceFloor: Double)
    fun clearRoundResults()
    fun biddingTimeoutReached()

    companion object {
        /**
         * How many succeeded result to hold
         */
        const val MaxAuctionResultsAmount = 2
    }
}
