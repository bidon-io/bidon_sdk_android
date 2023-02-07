package com.appodealstack.bidon.domain.auction.impl

import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.domain.adapter.AdSource
import com.appodealstack.bidon.domain.auction.*
import com.appodealstack.bidon.domain.auction.AdTypeParam
import com.appodealstack.bidon.domain.auction.Auction
import com.appodealstack.bidon.domain.auction.AuctionHolder
import com.appodealstack.bidon.domain.common.DemandAd
import com.appodealstack.bidon.domain.common.ext.*
import com.appodealstack.bidon.domain.stats.impl.logError
import com.appodealstack.bidon.domain.stats.impl.logInfo
import com.appodealstack.bidon.view.helper.SdkDispatchers
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
}

private const val Tag = "AuctionHolder"