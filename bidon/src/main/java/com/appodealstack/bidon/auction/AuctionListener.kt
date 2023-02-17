package com.appodealstack.bidon.auction
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface AuctionListener {
    fun onAuctionStarted() {}
    fun onAuctionSuccess(auctionResults: List<AuctionResult>) {}
    fun onAuctionFailed(error: Throwable) {}
}
