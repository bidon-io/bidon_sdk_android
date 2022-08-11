package com.appodealstack.bidon.core.impl

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.appodealstack.bidon.Core
import com.appodealstack.bidon.Core.SdkState
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.adapters.banners.AutoRefresh
import com.appodealstack.bidon.analytics.AdRevenueInterceptor
import com.appodealstack.bidon.analytics.AdRevenueInterceptorHolder
import com.appodealstack.bidon.analytics.AdRevenueLogger
import com.appodealstack.bidon.analytics.MediationNetwork
import com.appodealstack.bidon.auctions.domain.AdsRepository
import com.appodealstack.bidon.auctions.Auction
import com.appodealstack.bidon.auctions.domain.AuctionResolver
import com.appodealstack.bidon.auctions.AuctionResolversHolder
import com.appodealstack.bidon.core.AdaptersSource
import com.appodealstack.bidon.core.AutoRefresher
import com.appodealstack.bidon.core.InitializationCallback
import com.appodealstack.bidon.core.ListenersHolder
import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.di.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate

internal class CoreImpl(
    private val adsRepository: AdsRepository = get()
) : Core,
    AdaptersSource by get(),
    ListenersHolder by get(),
    AutoRefresher by get(),
    AdRevenueInterceptorHolder by get(),
    AuctionResolversHolder by get() {

    override val isInitialized: Boolean
        get() = sdkState.value == SdkState.Initialized

    override val isInitializing: Boolean
        get() = sdkState.value == SdkState.Initializing

    override val sdkState = MutableStateFlow(SdkState.NotInitialized)

    override fun init(activity: Activity, appKey: String, callback: InitializationCallback?) {
        if (sdkState.getAndUpdate { SdkState.Initializing } == SdkState.NotInitialized) {


            sdkState.value = SdkState.Initialized
        }
    }

    override fun loadAd(activity: Activity?, demandAd: DemandAd, adParams: Bundle) {
        if (!isInitialized) {
            logInternal(Tag, "Initialize Sdk before loading ad")
            return
        }
        if (!adsRepository.isAuctionActive(demandAd)) {
            val auction = get<Auction>().withResolver(getAuctionResolver(demandAd))
            adsRepository.saveAuction(demandAd, auction)
            auction.start(
                mediationRequests = setOf(),
                postBidRequests = setOf(),
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

    override fun saveAuctionResolver(demandAd: DemandAd, auctionResolver: AuctionResolver) {
        TODO("Not yet implemented")
    }

    override fun logAdRevenue(ad: Ad) {
        if (!isInitialized) {
            logInternal(Tag, "AdRevenue cannot be logged before Sdk is not initialized")
            return
        }
        val analytics = adapters.filterIsInstance<AdRevenueLogger>()
        if (analytics.isEmpty()) {
            logInternal(Tag, "AdRevenue's logger not initialized")
            return
        }
        val mediationNetwork = adapters.filterIsInstance<MediationNetwork>()
            .lastOrNull()?.mediationNetwork
        if (mediationNetwork == null) {
            logInternal(Tag, "Mediation demand not initialized")
            return
        }
        analytics.forEach {
            it.logAdRevenue(mediationNetwork, ad)
        }
    }

}

private const val Tag = "SdkCore"