package com.appodealstack.bidon.domain.auction

interface AuctionListener {
    fun auctionStarted() {}
    fun auctionSucceed(auctionResults: List<AuctionResult>) {}
    fun auctionFailed(error: Throwable) {}
}
