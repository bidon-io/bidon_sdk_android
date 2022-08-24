package com.appodealstack.bidon.auctions.domain.impl

import com.appodealstack.bidon.adapters.DemandAd
import com.appodealstack.bidon.auctions.data.models.AdTypeAdditional
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.domain.Auction
import com.appodealstack.bidon.auctions.domain.AuctionHolder
import com.appodealstack.bidon.auctions.domain.RoundsListener
import com.appodealstack.bidon.core.SdkDispatchers
import com.appodealstack.bidon.core.ext.asFailure
import com.appodealstack.bidon.core.ext.asSuccess
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInfo
import kotlinx.coroutines.*

internal class AuctionHolderImpl(
    private val auction: Auction,
    private val demandAd: DemandAd,
    private val roundsListener: RoundsListener,
) : AuctionHolder {
    private val dispatcher: CoroutineDispatcher = SdkDispatchers.Main
    private val scope: CoroutineScope get() = CoroutineScope(dispatcher)
    private val auctionResults = mutableListOf<AuctionResult>()
    private var auctionResultsDeferred: Deferred<Result<List<AuctionResult>>>? = null

    override val isActive: Boolean
        get() = auctionResultsDeferred?.isActive == true

    override val winner: AuctionResult?
        get() = auctionResults.firstOrNull()

    override fun startAuction(
        adTypeAdditional: AdTypeAdditional,
        onResult: (Result<List<AuctionResult>>) -> Unit
    ) {
        scope.launch {
            val deferred =
                auctionResultsDeferred ?: async {
                    auction.start(
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
                    logInfo(Tag, "Auction completed successfully: $results")
                    auctionResults.clear()
                    auctionResults.addAll(results)
                    onResult.invoke(results.asSuccess())
                }.onFailure {
                    logError(Tag, "Auction failed", it)
                    onResult.invoke(it.asFailure())
                }
        }
    }

    override fun destroy() {
        auctionResultsDeferred?.let {
            logInfo(Tag, "Auction canceled")
            it.cancel()
        }
        with(auctionResults) {
            forEach { it.adSource.destroy() }
            clear()
        }
    }
}

private const val Tag = "AuctionHolder"