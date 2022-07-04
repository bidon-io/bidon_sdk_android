package com.appodealstack.mads

import android.os.Bundle
import com.appodealstack.mads.analytics.AnalyticsSource
import com.appodealstack.mads.analytics.AnalyticsSourceImpl
import com.appodealstack.mads.auctions.AuctionResultsHolder
import com.appodealstack.mads.auctions.AuctionResultsHolderImpl
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
    )

    fun getListenerForDemand(adType: AdType): AdListener
    fun canShow(demandAd: DemandAd): Boolean
    fun destroyAd(demandAd: DemandAd, adParams: Bundle)
    fun setExtras(demandAd: DemandAd, adParams: Bundle)
    fun setListener(demandAd: DemandAd, adListener: AdListener)
    fun setRevenueListener(demandAd: DemandAd, adRevenueListener: AdRevenueListener)
}

internal class CoreImpl(
    private val auctionResultsHolder: AuctionResultsHolder = AuctionResultsHolderImpl()
) : Core,
    DemandsSource by DemandsSourceImpl(),
    AnalyticsSource by AnalyticsSourceImpl(),
    ListenersHolder by ListenersHolderImpl(),
    AuctionService by AuctionServiceImpl() {

    override fun loadAd(demandAd: DemandAd) {
        auctionResultsHolder.clearResults(demandAd)
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
                auctionResultsHolder.addResult(demandAd, success)
                auctionListener.onDemandAdLoaded(demandAd.adType, success)
            },
            onDemandLoadFailed = { failure ->
                auctionListener.onDemandAdLoadFailed(demandAd.adType, failure)
            },
            onAuctionFinished = {
                auctionResultsHolder.updateResults(demandAd, it)
                auctionListener.onWinnerFound(demandAd.adType, it)
                auctionListener.onAdLoaded(demandAd.adType, it.first())
            },
            onAuctionFailed = {
                auctionResultsHolder.clearResults(demandAd)
                auctionListener.onAdLoadFailed(demandAd.adType, it)
            }
        )
    }

    override fun showAd(demandAd: DemandAd, adParams: Bundle) {
        auctionResultsHolder.getTopResultOrNull(demandAd)?.objRequest?.showAd()
            ?: println("Not loaded Ad for: $demandAd")
    }

    override fun canShow(demandAd: DemandAd): Boolean {
        return auctionResultsHolder.getTopResultOrNull(demandAd)?.objRequest?.canShowAd() ?: false
    }

    override fun destroyAd(demandAd: DemandAd, adParams: Bundle) {
        return auctionResultsHolder.clearResults(demandAd)
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