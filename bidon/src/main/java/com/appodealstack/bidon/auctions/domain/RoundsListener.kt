package com.appodealstack.bidon.auctions.domain

import com.appodealstack.bidon.auctions.data.models.AuctionResult

interface RoundsListener {
    fun roundStarted(roundId: String) {}
    fun roundSucceed(roundId: String, roundResults: List<AuctionResult>) {}
    fun roundFailed(roundId: String, error: Throwable) {}
}
