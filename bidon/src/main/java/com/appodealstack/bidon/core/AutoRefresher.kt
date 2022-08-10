package com.appodealstack.bidon.core

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.appodealstack.bidon.auctions.AdsRepository
import com.appodealstack.bidon.auctions.AuctionListener
import com.appodealstack.bidon.auctions.NewAuction
import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.core.ext.retrieveAuctionRequests
import com.appodealstack.bidon.adapters.AdSource
import com.appodealstack.bidon.adapters.AdViewProvider
import com.appodealstack.bidon.adapters.Adapter
import com.appodealstack.bidon.adapters.DemandAd
import com.appodealstack.bidon.adapters.banners.AutoRefresh
import kotlinx.coroutines.*

internal interface AutoRefresher {
    fun delegateLoadingAdView(
        context: Context,
        demandAd: DemandAd,
        adParams: Bundle,
        autoRefresh: AutoRefresh,
        onViewReady: (View) -> Unit,
        adapters: List<Adapter>,
        auctionListener: AuctionListener,
        adContainer: ViewGroup?,
    )

    fun setAutoRefresh(demandAd: DemandAd, autoRefresh: AutoRefresh)
    fun cancelAutoRefresh(demandAd: DemandAd)
}

internal class AutoRefresherImpl(
    private val adsRepository: AdsRepository
) : AutoRefresher {
    private val scope get() = CoroutineScope(Dispatchers.Default)
    private var jobs = mutableMapOf<DemandAd, Job>()

    private var autoRefreshMap = mutableMapOf<DemandAd, AutoRefresh>()

    override fun delegateLoadingAdView(
        context: Context,
        demandAd: DemandAd,
        adParams: Bundle,
        autoRefresh: AutoRefresh,
        onViewReady: (View) -> Unit,
        adapters: List<Adapter>,
        auctionListener: AuctionListener,
        adContainer: ViewGroup?,
    ) {
        jobs[demandAd]?.cancel()
        autoRefreshMap[demandAd] = autoRefresh

        val repeat = {
            when (val param = autoRefreshMap[demandAd]) {
                AutoRefresh.Off, null -> {
                    // do nothing
                }
                is AutoRefresh.On -> {
                    scope.launch {
                        delay(param.timeoutMs)
                        (autoRefreshMap[demandAd] as? AutoRefresh.On)?.let {
                            delegateLoadingAdView(
                                context = context,
                                demandAd = demandAd,
                                adParams = adParams,
                                autoRefresh = it,
                                onViewReady = onViewReady,
                                adapters = adapters,
                                auctionListener = auctionListener,
                                adContainer = adContainer,
                            )
                        }
                    }
                }
            }
        }

        jobs[demandAd] = scope.launch {
            if (!adsRepository.isAuctionActive(demandAd)) {
                val auction = NewAuction
                adsRepository.saveAuction(demandAd, auction)
                auction.start(
                    mediationRequests = adapters
                        .filterIsInstance<AdSource.Banner>()
                        .retrieveAuctionRequests(context, demandAd, adParams, adContainer)
                        .toSet(),
                    postBidRequests = adapters
                        .filterIsInstance<AdSource.Banner>()
                        .retrieveAuctionRequests(context, demandAd, adParams, adContainer)
                        .toSet(),
                    onDemandLoaded = { auctionResult ->
                        auctionListener.demandAuctionSucceed(auctionResult)
                    },
                    onDemandLoadFailed = { throwable ->
                        auctionListener.demandAuctionFailed(demandAd, throwable)
                    },
                    onAuctionFailed = {
                        adsRepository.destroyResults(demandAd)
                        auctionListener.auctionFailed(demandAd, it)
                    },
                    onWinnerFound = {
                        auctionListener.winnerFound(it)
                        onViewReady.invoke((it.adProvider as AdViewProvider).getAdView())
                    },
                    onAuctionFinished = {
                        auctionListener.auctionSucceed(demandAd, it)
                        repeat()
                    },
                )
            } else {
                logInternal(Tag, "Auction is in progress for $demandAd")
            }
        }
    }

    override fun setAutoRefresh(demandAd: DemandAd, autoRefresh: AutoRefresh) {
        autoRefreshMap[demandAd] = autoRefresh
        when (autoRefresh) {
            AutoRefresh.Off -> cancelAutoRefresh(demandAd)
            is AutoRefresh.On -> {
                // do nothing
            }
        }
    }

    override fun cancelAutoRefresh(demandAd: DemandAd) {
        jobs[demandAd]?.cancel()
        jobs.remove(demandAd)
        autoRefreshMap.remove(demandAd)
    }

}

const val DefaultAutoRefreshTimeoutMs = 10_000L
private const val Tag = "AutoRefresher"