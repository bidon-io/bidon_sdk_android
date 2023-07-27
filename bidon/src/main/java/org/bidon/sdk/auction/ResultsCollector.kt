package org.bidon.sdk.auction

import org.bidon.sdk.auction.models.Bid
import org.bidon.sdk.auction.models.Round

/**
 * Created by Aleksei Cherniaev on 05/07/2023.
 */
internal interface ResultsCollector {
    fun startRound(round: Round, pricefloor: Double)
    fun serverBiddingStarted()
    fun serverBiddingFinished(bids: List<Bid>?)
    fun addNetworkResult(networkResult: AuctionResult.Network)
    fun addBiddingResult(biddingResult: AuctionResult.Bidding)
    fun getRoundResults(): RoundResult

    fun getAll(): List<AuctionResult>
    fun clear()
    suspend fun saveWinners(sourcePriceFloor: Double)
    fun clearRoundResults()
    fun biddingTimeoutReached(timeoutMs: Long)


    companion object {
        /**
         * How many succeeded result to hold
         */
        const val MaxAuctionResultsAmount = 2
    }
}
