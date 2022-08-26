package com.appodealstack.bidon.auctions.domain.impl

import com.appodealstack.bidon.adapters.AdSource
import com.appodealstack.bidon.adapters.DemandAd
import com.appodealstack.bidon.auctions.data.models.AdTypeAdditional
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.domain.Auction
import com.appodealstack.bidon.auctions.domain.AuctionHolder
import com.appodealstack.bidon.auctions.domain.RoundsListener
import com.appodealstack.bidon.core.SdkDispatchers
import com.appodealstack.bidon.core.ext.*
import com.appodealstack.bidon.di.get
import kotlinx.coroutines.*

internal class AuctionHolderImpl(
    private val demandAd: DemandAd,
    private val roundsListener: RoundsListener,
) : AuctionHolder {
    private val dispatcher: CoroutineDispatcher = SdkDispatchers.Main
    private val coroutineExceptionHandler by lazy {
        CoroutineExceptionHandler { _, exception ->
            logError(Tag, "CoroutineExceptionHandler", exception)
        }
    }
    private val scope: CoroutineScope
        get() = CoroutineScope(dispatcher + coroutineExceptionHandler)

    private val auctionResults = mutableListOf<AuctionResult>()
    private var auctionResultsDeferred: Deferred<Result<List<AuctionResult>>>? = null

    override val isActive: Boolean
        get() = auctionResultsDeferred?.isActive == true

    private var displayingWinner: AuctionResult? = null
    private var nextWinner: AuctionResult? = null

    override fun startAuction(
        adTypeAdditional: AdTypeAdditional,
        onResult: (Result<List<AuctionResult>>) -> Unit
    ) {
        scope.launch {
            val deferred =
                auctionResultsDeferred ?: async {
                    get<Auction>().start(
                        demandAd = demandAd,
                        resolver = MaxEcpmAuctionResolver,
                        adTypeAdditionalData = adTypeAdditional,
                        roundsListener = roundsListener
                    )
                }.also {
                    auctionResultsDeferred = it
                }
            deferred.await()
                .onSuccess { results ->
                    check(results.isNotEmpty()) {
                        "Auction succeed if results is not empty"
                    }
                    logInfo(Tag, "Auction completed successfully: $results")
                    nextWinner = results.first()
                    with(auctionResults) {
                        forEach { it.adSource.destroy() }
                        clear()
                        addAll(results)
                    }
                    onResult.invoke(results.asSuccess())
                }.onFailure {
                    nextWinner = null
                    logError(Tag, "Auction failed", it)
                    onResult.invoke(it.asFailure())
                }.onAny {
                    auctionResultsDeferred = null
                }
        }
    }

    override fun popWinner(): AdSource<*>? {
        displayingWinner?.adSource?.destroy()
        displayingWinner = nextWinner
        nextWinner = null
        return displayingWinner?.adSource
    }

    override fun destroy() {
        auctionResultsDeferred?.let {
            logInfo(Tag, "Auction canceled")
            it.cancel()
        }
        auctionResultsDeferred = null
        displayingWinner = null
        nextWinner = null
        with(auctionResults) {
            forEach { it.adSource.destroy() }
            clear()
        }
    }
}

private const val Tag = "AuctionHolder"