package com.appodealstack.bidon.domain.auction

interface RoundsListener {
    fun roundStarted(roundId: String) {}
    fun roundSucceed(roundId: String, roundResults: List<AuctionResult>) {}
    fun roundFailed(roundId: String, error: Throwable) {}
}
