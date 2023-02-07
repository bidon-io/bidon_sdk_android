package com.appodealstack.bidon.domain.auction
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface AuctionListener {
    fun auctionStarted() {}
    fun auctionSucceed(auctionResults: List<AuctionResult>) {}
    fun auctionFailed(error: Throwable) {}
}
