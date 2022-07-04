package com.appodealstack.mads

import android.os.Bundle
import com.appodealstack.mads.analytics.AnalyticsSource
import com.appodealstack.mads.analytics.AnalyticsSourceImpl
import com.appodealstack.mads.auctions.AuctionService
import com.appodealstack.mads.auctions.AuctionServiceImpl
import com.appodealstack.mads.base.AdType
import com.appodealstack.mads.demands.*
import com.appodealstack.mads.demands.listeners.ListenersHolder
import com.appodealstack.mads.demands.listeners.ListenersHolderImpl

val SdkCore: Core by lazy {
    CoreImpl()
}

interface Core {
    fun loadAd(demandAd: DemandAd)
    fun showAd(
        demandAd: DemandAd,
        adParams: Bundle,
        showItself: () -> Unit
    )

    fun getListenerForDemand(adType: AdType): AdListener
    fun canShow(demandAd: DemandAd): Boolean
    fun destroyAd(demandAd: DemandAd, adParams: Bundle)
    fun setExtras(demandAd: DemandAd, adParams: Bundle)
    fun setListener(demandAd: DemandAd, adListener: AdListener)
    fun setRevenueListener(demandAd: DemandAd, adRevenueListener: AdRevenueListener)
}

internal class CoreImpl : Core,
    DemandsSource by DemandsSourceImpl(),
    AnalyticsSource by AnalyticsSourceImpl(),
    ListenersHolder by ListenersHolderImpl(),
    AuctionService by AuctionServiceImpl() {
    override fun loadAd(demandAd: DemandAd) {
        startAuction(
            mediationRequests = demands
                .filterIsInstance<Demand.Mediation>()
                .map { it.createAuctionRequest(demandAd) }
                .toSet(),
            postBidRequests = demands
                .filterIsInstance<Demand.PostBid>()
                .map { it.createActionRequest() }
                .toSet(),
            onDemandLoaded = { success ->
                auctionListener.onDemandAdLoaded(demandAd.adType, success)
            },
            onDemandLoadFailed = { failure ->
                auctionListener.onDemandAdLoadFailed(demandAd.adType, failure)
            },
            onAuctionFinished = {
                auctionListener.onWinnerFound(demandAd.adType, it)
                auctionListener.onAdLoaded(demandAd.adType, it.first())
            },
            onAuctionFailed = {
                auctionListener.onAdLoadFailed(demandAd.adType, it)
            }
        )
    }

    override fun showAd(demandAd: DemandAd, adParams: Bundle, showItself: () -> Unit) {
        TODO("Not yet implemented")
    }

    override fun canShow(demandAd: DemandAd): Boolean {
        TODO("Not yet implemented")
    }

    override fun destroyAd(demandAd: DemandAd, adParams: Bundle) {
        TODO("Not yet implemented")
    }

    override fun setExtras(demandAd: DemandAd, adParams: Bundle) {
        TODO("Not yet implemented")
    }

    override fun setListener(demandAd: DemandAd, adListener: AdListener) {
        addUserListener(demandAd.adType, adListener)
    }

    override fun setRevenueListener(demandAd: DemandAd, adRevenueListener: AdRevenueListener) {
        TODO("Not yet implemented")
    }


}

private const val Tag = "SdkCore"