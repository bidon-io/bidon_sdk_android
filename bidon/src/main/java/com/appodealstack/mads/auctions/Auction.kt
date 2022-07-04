package com.appodealstack.mads.auctions

import com.appodealstack.mads.base.ext.logInternal
import com.appodealstack.mads.demands.DemandError
import kotlinx.coroutines.*
import kotlin.math.roundToInt

internal interface Auction {

    fun withComparator(comparator: Comparator<AuctionData.Success>): Auction

    fun startAuction(
        mediationRequests: Set<AuctionRequest.Mediation>,
        postBidRequests: Set<AuctionRequest.PostBid>,
        onDemandLoaded: (intermediateResult: AuctionData.Success) -> Unit,
        onDemandLoadFailed: (intermediateResult: AuctionData.Failure) -> Unit,
        onAuctionFinished: (allResults: List<AuctionData.Success>) -> Unit,
        onAuctionFailed: (cause: Throwable) -> Unit,
    )
}

internal class AuctionImpl : Auction {
    private var comparator: Comparator<AuctionData.Success> = DefaultPriceFloorComparator
    private val scope get() = CoroutineScope(Dispatchers.Default)

    override fun withComparator(comparator: Comparator<AuctionData.Success>): Auction {
        this.comparator = comparator
        return this
    }

    override fun startAuction(
        mediationRequests: Set<AuctionRequest.Mediation>,
        postBidRequests: Set<AuctionRequest.PostBid>,
        onDemandLoaded: (intermediateResult: AuctionData.Success) -> Unit,
        onDemandLoadFailed: (intermediateResult: AuctionData.Failure) -> Unit,
        onAuctionFinished: (allResults: List<AuctionData.Success>) -> Unit,
        onAuctionFailed: (cause: Throwable) -> Unit,
    ) {
        scope.launch {
            runAuction(
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

    private suspend fun runAuction(
        mediationRequests: Set<AuctionRequest.Mediation>,
        postBidRequests: Set<AuctionRequest.PostBid>,
        onDemandLoaded: (intermediateResult: AuctionData.Success) -> Unit,
        onDemandLoadFailed: (intermediateResult: AuctionData.Failure) -> Unit,
    ): Result<List<AuctionData.Success>> = coroutineScope {
        val auctionResults = mutableListOf<AuctionData.Success>()
        logInternal(Tag, "Round 1 (Mediation) started with: $mediationRequests")

        /**
         * First round - Mediation
         */
        val mediationResults = mediationRequests.mapNotNull { mediation ->
            withTimeoutOrNull(RequestTimeout) {
                async {
                    when (val auctionResult = mediation.execute()) {
                        is AuctionData.Success -> {
                            with(auctionResults) {
                                add(auctionResult)
                                sortWith(comparator)
                            }
                            onDemandLoaded.invoke(auctionResult)
                        }
                        is AuctionData.Failure -> {
                            onDemandLoadFailed.invoke(auctionResult)
                        }
                    }
                }
            }
        }
        mediationResults.forEach { it.await() }

        val mediationWinner = auctionResults.firstOrNull()

        logInternal(Tag, "Round 2 (PostBid) started with: $postBidRequests")

        /**
         * Second round - PostBid
         */
        val postBidResults = postBidRequests.mapNotNull { postBid ->
            withTimeoutOrNull(RequestTimeout) {
                async {
                    val auctionResult = postBid.execute(
                        mediationWinner?.price?.let {
                            AuctionRequest.AdditionalData(priceFloor = it)
                        }
                    )
                    when (auctionResult) {
                        is AuctionData.Success -> {
                            with(auctionResults) {
                                add(auctionResult)
                                sortWith(comparator)
                            }
                            onDemandLoaded.invoke(auctionResult)
                        }
                        is AuctionData.Failure -> {
                            onDemandLoadFailed.invoke(auctionResult)
                        }
                    }
                }
            }
        }
        postBidResults.forEach { it.await() }

        /**
         * Result of auction
         */

        logInternal(Tag, "Finished with ${auctionResults.size} results")
        auctionResults.forEach {
            logInternal(Tag, "Finished result: $it")
        }
        if (auctionResults.isNotEmpty()) {
            Result.success(auctionResults)
        } else {
            Result.failure(DemandError.NoFill)
        }
    }
}

internal val DefaultPriceFloorComparator = Comparator<AuctionData.Success> { result1, result2 ->
    (((result2?.price ?: 0.0) - (result1?.price ?: 0.0)) * 1_000_000).roundToInt()
}

private const val RequestTimeout = 5000L
private const val Tag = "[Auction]"