package com.appodealstack.mads

import android.app.Activity
import android.os.Bundle
import com.appodealstack.mads.analytics.AnalyticsSource
import com.appodealstack.mads.analytics.AnalyticsSourceImpl
import com.appodealstack.mads.auctions.*
import com.appodealstack.mads.auctions.AuctionsHolder
import com.appodealstack.mads.auctions.AuctionsHolderImpl
import com.appodealstack.mads.base.ext.logInternal
import com.appodealstack.mads.demands.*
import com.appodealstack.mads.demands.ListenersHolder
import com.appodealstack.mads.demands.ListenersHolderImpl

val SdkCore: Core by lazy {
    CoreImpl()
}

interface Core {
    fun loadAd(demandAd: DemandAd)
    fun showAd(activity: Activity?, demandAd: DemandAd, adParams: Bundle)

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
    private val auctionsHolder: AuctionsHolder = AuctionsHolderImpl()
) : Core,
    DemandsSource by DemandsSourceImpl(),
    AnalyticsSource by AnalyticsSourceImpl(),
    ListenersHolder by ListenersHolderImpl() {

    override fun loadAd(demandAd: DemandAd) {
        if (!auctionsHolder.isAuctionActive(demandAd)) {
            val auction = NewAuction
            auctionsHolder.addAuction(demandAd, auction)
            auction.start(
                mediationRequests = demands
                    .filterIsInstance<Demand.Mediation>()
                    .map { it.createAuctionRequest(demandAd) }
                    .toSet(),
                postBidRequests = demands
                    .filterIsInstance<Demand.PostBid>()
                    .map { it.createActionRequest(ownerDemandAd = demandAd) }
                    .toSet(),
                onDemandLoaded = { success ->
                    auctionListener.onDemandAdLoaded(demandAd, success)
                },
                onDemandLoadFailed = { failure ->
                    auctionListener.onDemandAdLoadFailed(demandAd, failure)
                },
                onAuctionFinished = {
                    auctionListener.onWinnerFound(demandAd, it)
                    auctionListener.onAdLoaded(demandAd, it.first())
                },
                onAuctionFailed = {
                    auctionsHolder.clearResults(demandAd)
                    auctionListener.onAdLoadFailed(demandAd, it)
                }
            )
        } else {
            logInternal(Tag, "Auction is in progress for $demandAd")
        }
    }

    override fun showAd(activity: Activity?, demandAd: DemandAd, adParams: Bundle) {
        auctionsHolder.getTopResultOrNull(demandAd)?.objRequest?.showAd(activity, adParams)
            ?: logInternal(Tag, "Not loaded Ad for: $demandAd")
    }

    override fun canShow(demandAd: DemandAd): Boolean {
        return auctionsHolder.getTopResultOrNull(demandAd)?.objRequest?.canShowAd() ?: false
    }

    override fun destroyAd(demandAd: DemandAd, adParams: Bundle) {
        addUserListener(demandAd, null)
        return auctionsHolder.clearResults(demandAd)
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