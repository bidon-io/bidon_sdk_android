package com.appodealstack.mads.auctions

import com.appodealstack.mads.auctions.impl.DefaultAuctionResolver
import com.appodealstack.mads.core.ext.logInternal
import com.appodealstack.mads.demands.DemandError
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

internal val NewAuction: Auction get() = AuctionImpl()

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

internal class AuctionImpl : Auction {
    private var auctionResolver: AuctionResolver = DefaultAuctionResolver()
    private val scope get() = CoroutineScope(Dispatchers.Default)
    private val auctionResults = MutableStateFlow(listOf<AuctionResult>())
    private val isAuctionActive = MutableStateFlow(true)

    override fun withResolver(auctionResolver: AuctionResolver): Auction {
        this.auctionResolver = auctionResolver
        return this
    }

    override fun start(
        mediationRequests: Set<AuctionRequest>,
        postBidRequests: Set<AuctionRequest>,
        onDemandLoaded: (intermediateResult: AuctionResult) -> Unit,
        onDemandLoadFailed: (intermediateResult: Throwable) -> Unit,
        onAuctionFinished: (allResults: List<AuctionResult>) -> Unit,
        onAuctionFailed: (cause: Throwable) -> Unit,
        onWinnerFound: (AuctionResult) -> Unit,
    ) {
        scope.launch {
            runAuction(
                mediationRequests = mediationRequests,
                postBidRequests = postBidRequests,
                onDemandLoaded = onDemandLoaded,
                onDemandLoadFailed = onDemandLoadFailed,
                onAuctionFinished = onAuctionFinished,
                onAuctionFailed = onAuctionFailed,
                onWinnerFound = onWinnerFound
            )
            isAuctionActive.value = false
        }
    }

    override fun getWinnerOrNull(onWinnerFound: (AuctionResult?) -> Unit) {
        scope.launch(Dispatchers.Main) {
            onWinnerFound.invoke(auctionResolver.findWinner(auctionResults.value))
        }
    }

    override fun isAuctionActive(): Boolean = isAuctionActive.value

    override suspend fun awaitAndGetResults(): Result<List<AuctionResult>> {
        isAuctionActive.first { !it }
        return if (auctionResults.value.isNotEmpty()) {
            Result.success(auctionResults.value)
        } else {
            Result.failure(Exception("No auction result"))
        }
    }

    override fun getResults(): List<AuctionResult> = auctionResults.value

    private suspend fun runAuction(
        mediationRequests: Set<AuctionRequest>,
        postBidRequests: Set<AuctionRequest>,
        onDemandLoaded: (intermediateResult: AuctionResult) -> Unit,
        onDemandLoadFailed: (intermediateResult: Throwable) -> Unit,
        onAuctionFinished: (allResults: List<AuctionResult>) -> Unit,
        onAuctionFailed: (cause: Throwable) -> Unit,
        onWinnerFound: (AuctionResult) -> Unit,
    ) = coroutineScope {
        logInternal(Tag, "Round 1 (Mediation) started with: $mediationRequests")

        /**
         * First round - Mediation
         */
        val mediationResults = mediationRequests.map { mediation ->
            async {
                withTimeoutOrNull(RequestTimeout) {
                    mediation.execute(data = null)
                } ?: Result.failure(DemandError.NetworkTimeout(null))
            }
        }
        mediationResults.forEach { deferred ->
            deferred.await()
                .onSuccess { auctionResult ->
                    auctionResults.update { (it + auctionResult) }
                    onDemandLoaded.invoke(auctionResult)
                }.onFailure {
                    onDemandLoadFailed.invoke(it)
                }

        }
        val mediationWinner = auctionResolver.findWinner(auctionResults.value)

        logInternal(Tag, "Round 2 (PostBid) started with: $postBidRequests")

        /**
         * Second round - PostBid
         */
        val postBidResults = postBidRequests.map { postBid ->
            async {
                withTimeoutOrNull(RequestTimeout) {
                    postBid.execute(
                        mediationWinner?.ad?.price?.let {
                            AuctionRequest.Data(priceFloor = it)
                        }
                    )
                } ?: Result.failure(DemandError.NetworkTimeout(null))
            }
        }
        postBidResults.forEach { deferred ->
            deferred.await()
                .onSuccess { auctionResult ->
                    auctionResults.update { (it + auctionResult) }
                    onDemandLoaded.invoke(auctionResult)
                }.onFailure {
                    onDemandLoadFailed.invoke(it)
                }
        }

        /**
         * Result of auction
         */
        logInternal(Tag, "Finished with ${auctionResults.value.size} results")
        auctionResults.value.forEach {
            logInternal(Tag, "Finished result: $it")
        }

        val winner = auctionResolver.findWinner(auctionResults.value)

        withContext(Dispatchers.Main) {
            if (winner != null) {
                onAuctionFinished(auctionResults.value)
                onWinnerFound(winner)
            } else {
                onAuctionFailed(DemandError.NoFill(null))
            }
        }
    }
}

private const val RequestTimeout = 5000L
private const val Tag = "[Auction]"