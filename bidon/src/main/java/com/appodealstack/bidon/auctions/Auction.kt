package com.appodealstack.bidon.auctions

import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.domain.AuctionRequest
import com.appodealstack.bidon.auctions.domain.AuctionResolver

@Deprecated("")
internal interface Auction {

    fun withResolver(auctionResolver: AuctionResolver): Auction

    fun start(
        mediationRequests: Set<AuctionRequest>,
        postBidRequests: Set<AuctionRequest>,
        onDemandLoaded: (intermediateResult: AuctionResult) -> Unit,
        onDemandLoadFailed: (intermediateResult: Throwable) -> Unit,
        onAuctionFinished: (allResults: List<AuctionResult>) -> Unit,
        onAuctionFailed: (cause: Throwable) -> Unit,
        onWinnerFound: (AuctionResult) -> Unit,
    )

    fun getWinnerOrNull(onWinnerFound: (AuctionResult?) -> Unit)
    fun isAuctionActive(): Boolean
    suspend fun awaitAndGetResults(): Result<List<AuctionResult>>
    fun getResults(): List<AuctionResult>
}

