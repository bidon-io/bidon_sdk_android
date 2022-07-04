package com.appodealstack.mads

import android.os.Bundle
import com.appodealstack.mads.analytics.AnalyticsSource
import com.appodealstack.mads.analytics.AnalyticsSourceImpl
import com.appodealstack.mads.auctions.*
import com.appodealstack.mads.auctions.AuctionImpl
import com.appodealstack.mads.auctions.AuctionResultsHolder
import com.appodealstack.mads.auctions.AuctionResultsHolderImpl
import com.appodealstack.mads.base.ext.logInternal
import com.appodealstack.mads.demands.*
import com.appodealstack.mads.demands.ListenersHolder
import com.appodealstack.mads.demands.ListenersHolderImpl

val SdkCore: Core by lazy {
    CoreImpl()
}

interface Core {
    fun loadAd(demandAd: DemandAd)
    fun showAd(
        demandAd: DemandAd,
        adParams: Bundle,
    )

    fun canShow(demandAd: DemandAd): Boolean
    fun destroyAd(demandAd: DemandAd, adParams: Bundle)
    fun setExtras(demandAd: DemandAd, adParams: Bundle)
    fun setListener(demandAd: DemandAd, adListener: AdListener)
    fun setRevenueListener(demandAd: DemandAd, adRevenueListener: AdRevenueListener)

    /**
     * implemented at [ListenersHolderImpl]
     */
    fun getListenerForDemand(demandAd: DemandAd): AdListener
}

internal class CoreImpl(
    private val auctionResultsHolder: AuctionResultsHolder = AuctionResultsHolderImpl()
) : Core,
    DemandsSource by DemandsSourceImpl(),
    AnalyticsSource by AnalyticsSourceImpl(),
    ListenersHolder by ListenersHolderImpl(),
    Auction by AuctionImpl() {

    override fun loadAd(demandAd: DemandAd) {
        auctionResultsHolder.clearResults(demandAd)
        startAuction(
            mediationRequests = demands
                .filterIsInstance<Demand.Mediation>()
                .map { it.createAuctionRequest(demandAd) }
                .toSet(),
            postBidRequests = demands
                .filterIsInstance<Demand.PostBid>()
                .map { it.createActionRequest(ownerDemandAd = demandAd) }
                .toSet(),
            onDemandLoaded = { success ->
                auctionResultsHolder.addResult(demandAd, success)
                auctionListener.onDemandAdLoaded(demandAd, success)
            },
            onDemandLoadFailed = { failure ->
                auctionListener.onDemandAdLoadFailed(demandAd, failure)
            },
            onAuctionFinished = {
                auctionResultsHolder.updateResults(demandAd, it)
                auctionListener.onWinnerFound(demandAd, it)
                auctionListener.onAdLoaded(demandAd, it.first())
            },
            onAuctionFailed = {
                auctionResultsHolder.clearResults(demandAd)
                auctionListener.onAdLoadFailed(demandAd, it)
            }
        )
    }

    override fun showAd(demandAd: DemandAd, adParams: Bundle) {
        auctionResultsHolder.getTopResultOrNull(demandAd)?.objRequest?.showAd(adParams)
            ?: logInternal(Tag, "Not loaded Ad for: $demandAd")
    }

    override fun canShow(demandAd: DemandAd): Boolean {
        return auctionResultsHolder.getTopResultOrNull(demandAd)?.objRequest?.canShowAd() ?: false
    }

    override fun destroyAd(demandAd: DemandAd, adParams: Bundle) {
        addUserListener(demandAd, null)
        return auctionResultsHolder.clearResults(demandAd)
    }

    override fun setExtras(demandAd: DemandAd, adParams: Bundle) {
        TODO("Not yet implemented")
    }

    override fun setListener(demandAd: DemandAd, adListener: AdListener) {
        addUserListener(demandAd, adListener)
    }

    override fun setRevenueListener(demandAd: DemandAd, adRevenueListener: AdRevenueListener) {
        TODO("Not yet implemented")
    }


}

private const val Tag = "SdkCore"