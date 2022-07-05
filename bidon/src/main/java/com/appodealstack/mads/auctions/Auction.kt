package com.appodealstack.mads.auctions

import com.appodealstack.mads.base.ext.logInternal
import com.appodealstack.mads.demands.DemandError
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

internal val NewAuction: Auction get() = AuctionImpl()

internal interface Auction {

    fun withComparator(comparator: Comparator<AuctionData.Success>): Auction

    fun start(
        mediationRequests: Set<AuctionRequest.Mediation>,
        postBidRequests: Set<AuctionRequest.PostBid>,
        onDemandLoaded: (intermediateResult: AuctionData.Success) -> Unit,
        onDemandLoadFailed: (intermediateResult: AuctionData.Failure) -> Unit,
        onAuctionFinished: (allResults: List<AuctionData.Success>) -> Unit,
        onAuctionFailed: (cause: Throwable) -> Unit,
    )

    fun getTopResultOrNull(): AuctionData.Success?
    fun isAuctionActive(): Boolean
    suspend fun awaitAndGetResults(): Result<List<AuctionData.Success>>
}

internal class AuctionImpl : Auction {
    private var comparator: Comparator<AuctionData.Success> = DefaultPriceFloorComparator
    private val scope get() = CoroutineScope(Dispatchers.Default)
    private val auctionResults = MutableStateFlow(listOf<AuctionData.Success>())
    private val isAuctionActive = MutableStateFlow(true)

    override fun withComparator(comparator: Comparator<AuctionData.Success>): Auction {
        this.comparator = comparator
        return this
    }

    override fun start(
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
            isAuctionActive.value = false
        }
    }

    override fun getTopResultOrNull(): AuctionData.Success? {
        return auctionResults.value.firstOrNull()
    }

    override fun isAuctionActive(): Boolean = isAuctionActive.value

    override suspend fun awaitAndGetResults(): Result<List<AuctionData.Success>> {
        isAuctionActive.first { !it }
        return if (auctionResults.value.isNotEmpty()) {
            Result.success(auctionResults.value)
        } else {
            Result.failure(Exception("No auction result"))
        }
    }

    private suspend fun runAuction(
        mediationRequests: Set<AuctionRequest.Mediation>,
        postBidRequests: Set<AuctionRequest.PostBid>,
        onDemandLoaded: (intermediateResult: AuctionData.Success) -> Unit,
        onDemandLoadFailed: (intermediateResult: AuctionData.Failure) -> Unit,
    ): Result<List<AuctionData.Success>> = coroutineScope {
        logInternal(Tag, "Round 1 (Mediation) started with: $mediationRequests")

        /**
         * First round - Mediation
         */
        val mediationResults = mediationRequests.map { mediation ->
            async {
                withTimeoutOrNull(RequestTimeout) {
                    when (val auctionResult = mediation.execute()) {
                        is AuctionData.Success -> {
                            auctionResults.update {
                                (it + auctionResult).sortedWith(comparator)
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

        val mediationWinner = auctionResults.value.firstOrNull()

        logInternal(Tag, "Round 2 (PostBid) started with: $postBidRequests")

        /**
         * Second round - PostBid
         */
        val postBidResults = postBidRequests.map { postBid ->
            async {
                withTimeoutOrNull(RequestTimeout) {
                    val auctionResult = postBid.execute(
                        mediationWinner?.price?.let {
                            AuctionRequest.AdditionalData(priceFloor = it)
                        }
                    )
                    when (auctionResult) {
                        is AuctionData.Success -> {
                            auctionResults.update {
                                (it + auctionResult).sortedWith(comparator)
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
        logInternal(Tag, "Finished with ${auctionResults.value.size} results")
        auctionResults.value.forEach {
            logInternal(Tag, "Finished result: $it")
        }
        if (auctionResults.value.isNotEmpty()) {
            Result.success(auctionResults.value)
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