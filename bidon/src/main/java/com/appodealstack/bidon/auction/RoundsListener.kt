package com.appodealstack.bidon.auction
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface RoundsListener {
    fun roundStarted(roundId: String) {}
    fun roundSucceed(roundId: String, roundResults: List<AuctionResult>) {}
    fun roundFailed(roundId: String, error: Throwable) {}
}
