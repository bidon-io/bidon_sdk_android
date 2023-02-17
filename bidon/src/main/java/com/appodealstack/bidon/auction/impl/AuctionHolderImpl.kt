package com.appodealstack.bidon.auction.impl

import com.appodealstack.bidon.adapter.AdSource
import com.appodealstack.bidon.adapter.DemandAd
import com.appodealstack.bidon.auction.*
import com.appodealstack.bidon.auction.AdTypeParam
import com.appodealstack.bidon.auction.Auction
import com.appodealstack.bidon.auction.AuctionHolder
import com.appodealstack.bidon.logs.logging.impl.logError
import com.appodealstack.bidon.logs.logging.impl.logInfo
import com.appodealstack.bidon.utils.SdkDispatchers
import com.appodealstack.bidon.utils.di.get
import com.appodealstack.bidon.utils.ext.asFailure
import com.appodealstack.bidon.utils.ext.asSuccess
import com.appodealstack.bidon.utils.ext.onAny
import kotlinx.coroutines.*

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
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

    private var auctionResultsDeferred: Deferred<Result<List<AuctionResult>>>? = null

    override val isActive: Boolean
        get() = auctionResultsDeferred?.isActive == true

    private var displayingWinner: AuctionResult? = null
    private var nextWinner: AuctionResult? = null

    override fun startAuction(
        adTypeParam: AdTypeParam,
        onResult: (Result<List<AuctionResult>>) -> Unit
    ) {
        scope.launch {
            val deferred =
                auctionResultsDeferred ?: async {
                    get<Auction>().start(
                        demandAd = demandAd,
                        resolver = MaxEcpmAuctionResolver,
                        adTypeParamData = adTypeParam,
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
        displayingWinner?.adSource?.destroy()
        displayingWinner = null
        nextWinner?.adSource?.destroy()
        nextWinner = null
    }

    override fun isAdReady(): Boolean {
        return nextWinner?.adSource?.isAdReadyToShow == true
    }
}

private const val Tag = "AuctionHolder"