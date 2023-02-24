package org.bidon.sdk.auction
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface RoundsListener {
    fun onRoundStarted(roundId: String, pricefloor: Double) {}
    fun onRoundSucceed(roundId: String, roundResults: List<AuctionResult>) {}
    fun onRoundFailed(roundId: String, error: Throwable) {}
}
