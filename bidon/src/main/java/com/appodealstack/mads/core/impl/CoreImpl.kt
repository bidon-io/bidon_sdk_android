package com.appodealstack.mads.core.impl

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.appodealstack.mads.Core
import com.appodealstack.mads.analytics.AdRevenueInterceptor
import com.appodealstack.mads.analytics.AdRevenueInterceptorHolder
import com.appodealstack.mads.analytics.AdRevenueInterceptorHolderImpl
import com.appodealstack.mads.analytics.AdRevenueLogger
import com.appodealstack.mads.auctions.AdsRepository
import com.appodealstack.mads.auctions.AdsRepositoryImpl
import com.appodealstack.mads.auctions.NewAuction
import com.appodealstack.mads.core.*
import com.appodealstack.mads.core.ext.logInternal
import com.appodealstack.mads.core.ext.retrieveAuctionRequests
import com.appodealstack.mads.demands.*
import com.appodealstack.mads.demands.banners.AutoRefresh

internal class CoreImpl(
    private val adsRepository: AdsRepository = AdsRepositoryImpl()
) : Core,
    DemandsSource by DemandsSourceImpl(),
    AnalyticsSource by AnalyticsSourceImpl(),
    ListenersHolder by ListenersHolderImpl(),
    AutoRefresher by AutoRefresherImpl(adsRepository),
    AdRevenueInterceptorHolder by AdRevenueInterceptorHolderImpl() {

    override var isInitialized: Boolean = false

    override fun loadAd(activity: Activity?, demandAd: DemandAd, adParams: Bundle) {
        if (!isInitialized) {
            logInternal(Tag, "Initialize Sdk before loading ad")
            return
        }
        if (!adsRepository.isAuctionActive(demandAd)) {
            val auction = NewAuction
            adsRepository.saveAuction(demandAd, auction)
            auction.start(
                mediationRequests = adapters
                    .filterIsInstance<Adapter.Mediation<*>>()
                    .retrieveAuctionRequests(activity, demandAd, adParams)
                    .toSet(),
                postBidRequests = adapters
                    .filterIsInstance<Adapter.PostBid<*>>()
                    .retrieveAuctionRequests(activity, demandAd, adParams)
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
                onAuctionFinished = {
                    auctionListener.auctionSucceed(demandAd, it)
                },
                onWinnerFound = {
                    auctionListener.winnerFound(it)
                }
            )
        } else {
            logInternal(Tag, "Auction is in progress for $demandAd")
        }
    }

    override fun loadAdView(
        context: Context,
        demandAd: DemandAd,
        adParams: Bundle,
        autoRefresh: AutoRefresh,
        onViewReady: (View) -> Unit,
        adContainer: ViewGroup?,
    ) {
        if (!isInitialized) {
            logInternal(Tag, "Initialize Sdk before loading ad")
            return
        }
        delegateLoadingAdView(
            context = context,
            demandAd = demandAd,
            adParams = adParams,
            autoRefresh = autoRefresh,
            onViewReady = onViewReady,
            adapters = adapters,
            auctionListener = auctionListener,
            adContainer = adContainer
        )
    }

    override fun showAd(activity: Activity?, demandAd: DemandAd, adParams: Bundle) {
        adsRepository.getWinnerOrNull(
            demandAd = demandAd,
            onWinnerFound = { auctionData ->
                if (auctionData != null) {
                    auctionData.adProvider.showAd(activity, adParams)
                } else {
                    logInternal(Tag, "Not loaded Ad for: $demandAd")
                }
            }
        )
    }

    override fun canShow(demandAd: DemandAd): Boolean {
        return adsRepository.getResults(demandAd).firstOrNull()?.adProvider?.canShow() ?: false
    }

    override fun destroyAd(demandAd: DemandAd, adParams: Bundle) {
        addUserListener(demandAd, null)
        cancelAutoRefresh(demandAd)
        return adsRepository.destroyResults(demandAd)
    }

    override fun setExtras(demandAd: DemandAd, adParams: Bundle) {
        if (!isInitialized) {
            logInternal(Tag, "Extra data cannot be set before Sdk is not initialized")
            return
        }
        /**
         * Set extras for all new Ad objects
         */
        adapters.filterIsInstance<ExtrasSource>().forEach { extrasSource ->
            extrasSource.setExtras(demandAd, adParams)
        }
        /**
         * Set extras for all existing Ad objects
         */
        adsRepository.getResults(demandAd).forEach { auctionResult ->
            (auctionResult.adProvider as? ExtrasProvider)?.setExtras(adParams)
        }
    }

    override fun setListener(demandAd: DemandAd, adListener: AdListener) {
        addUserListener(demandAd, adListener)
    }

    override fun setRevenueListener(demandAd: DemandAd, adRevenueListener: AdRevenueListener) {
        if (!isInitialized) {
            logInternal(Tag, "RevenueListener cannot be set before Sdk is not initialized")
            return
        }
        /**
         * Set listener for all new Ad objects
         */
        adapters.filterIsInstance<AdRevenueSource>().forEach { adRevenueSource ->
            adRevenueSource.setAdRevenueListener(demandAd, adRevenueListener)
        }
        /**
         * Set listener for all existing Ad objects
         */
        adsRepository.getResults(demandAd).forEach { auctionResult ->
            (auctionResult.adProvider as? AdRevenueProvider)?.setAdRevenueListener(adRevenueListener)
        }
    }

    override fun getAdRevenueInterceptor(): AdRevenueInterceptor {
        return obtainAdRevenueInterceptor()
    }

    override fun getPlacement(demandAd: DemandAd): String? {
        val loadedAdPlacement = adsRepository.getResults(demandAd)
            .map { it.adProvider }.filterIsInstance<PlacementProvider>().firstOrNull()
            ?.getPlacement()
        if (loadedAdPlacement != null) return loadedAdPlacement
        @Suppress("UnnecessaryVariable")
        val savedPlacement = adapters
            .filterIsInstance<PlacementSource>().firstOrNull()
            ?.getPlacement(demandAd)
        return savedPlacement
    }

    override fun setPlacement(demandAd: DemandAd, placement: String?) {
        if (!isInitialized) {
            logInternal(Tag, "Placement cannot be saved before Sdk is not initialized")
            return
        }
        /**
         * Set placement for all new Ad objects
         */
        adapters.filterIsInstance<PlacementSource>().forEach { placementSource ->
            placementSource.setPlacement(demandAd, placement)
        }
        /**
         * Set placement for all existing Ad objects
         */
        adsRepository.getResults(demandAd).forEach { auctionResult ->
            (auctionResult.adProvider as? PlacementProvider)?.setPlacement(placement)
        }
    }

    override fun logAdRevenue(ad: Ad) {
        if (!isInitialized) {
            logInternal(Tag, "AdRevenue cannot be logged before Sdk is not initialized")
            return
        }
        if (analytics.isEmpty()) {
            logInternal(Tag, "AdRevenue's logger not initialized")
            return
        }
        val mediationNetwork = adapters.filterIsInstance<Adapter.Mediation<*>>()
            .lastOrNull()?.mediationNetwork
        if (mediationNetwork == null) {
            logInternal(Tag, "Mediation demand not initialized")
            return
        }
        analytics.filterIsInstance<AdRevenueLogger>()
            .forEach {
                it.logAdRevenue(mediationNetwork, ad)
            }
    }

}

private const val Tag = "SdkCore"