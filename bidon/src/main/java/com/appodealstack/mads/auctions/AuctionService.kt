package com.appodealstack.mads.auctions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface AuctionService {
    fun startAuction(
        mediationRequests: Set<AuctionRequest.Mediation>,
        postBidRequests: Set<AuctionRequest.PostBid>,
        onDemandLoaded: (intermediateResult: AuctionData.Success) -> Unit,
        onDemandLoadFailed: (intermediateResult: AuctionData.Failure) -> Unit,
        onAuctionFinished: (allResults: List<AuctionData.Success>) -> Unit,
        onAuctionFailed: (cause: Throwable) -> Unit,
    )
}

internal class AuctionServiceImpl : AuctionService {
    private val auction: Auction get() = AuctionImpl()
    private val scope get() = CoroutineScope(Dispatchers.Default)

    override fun startAuction(
        mediationRequests: Set<AuctionRequest.Mediation>,
        postBidRequests: Set<AuctionRequest.PostBid>,
        onDemandLoaded: (intermediateResult: AuctionData.Success) -> Unit,
        onDemandLoadFailed: (intermediateResult: AuctionData.Failure) -> Unit,
        onAuctionFinished: (allResults: List<AuctionData.Success>) -> Unit,
        onAuctionFailed: (cause: Throwable) -> Unit,
    ) {
        scope.launch {
            auction.runAuction(
                mediationRequests = mediationRequests,
                postBidRequests = postBidRequests,
                onDemandLoaded = onDemandLoaded,
                onDemandLoadFailed = onDemandLoadFailed
            ).onSuccess {
                onAuctionFinished.invoke(it)
            }.onFailure {
                // no one demand has loaded Ad
                onAuctionFailed.invoke(it)
            }
        }
    }
}