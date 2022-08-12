package com.appodealstack.bidon.auctions.domain

import com.appodealstack.bidon.auctions.data.models.AuctionResult

interface NewAuctionListener {
    fun auctionStarted()
    fun auctionSucceed(auctionResults: List<AuctionResult>)
    fun auctionFailed(error: Throwable)
}