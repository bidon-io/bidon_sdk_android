package com.appodealstack.bidon.auctions

import com.appodealstack.bidon.auctions.data.models.OldAuctionResult
import com.appodealstack.bidon.auctions.domain.OldAuctionRequest
import com.appodealstack.bidon.auctions.domain.AuctionResolver

@Deprecated("")
internal interface Auction {

    fun withResolver(auctionResolver: AuctionResolver): Auction

    fun start(
        mediationRequests: Set<OldAuctionRequest>,
        postBidRequests: Set<OldAuctionRequest>,
        onDemandLoaded: (intermediateResult: OldAuctionResult) -> Unit,
        onDemandLoadFailed: (intermediateResult: Throwable) -> Unit,
        onAuctionFinished: (allResults: List<OldAuctionResult>) -> Unit,
        onAuctionFailed: (cause: Throwable) -> Unit,
        onWinnerFound: (OldAuctionResult) -> Unit,
    )

    fun getWinnerOrNull(onWinnerFound: (OldAuctionResult?) -> Unit)
    fun isAuctionActive(): Boolean
    suspend fun awaitAndGetResults(): Result<List<OldAuctionResult>>
    fun getResults(): List<OldAuctionResult>
}

