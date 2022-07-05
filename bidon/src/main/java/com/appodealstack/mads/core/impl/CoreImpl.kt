package com.appodealstack.mads.core.impl

import android.app.Activity
import android.os.Bundle
import com.appodealstack.mads.Core
import com.appodealstack.mads.auctions.AuctionsHolder
import com.appodealstack.mads.auctions.AuctionsHolderImpl
import com.appodealstack.mads.auctions.NewAuction
import com.appodealstack.mads.core.AnalyticsSource
import com.appodealstack.mads.core.DemandsSource
import com.appodealstack.mads.core.ListenersHolder
import com.appodealstack.mads.core.ext.logInternal
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.AdRevenueListener
import com.appodealstack.mads.demands.Demand
import com.appodealstack.mads.demands.DemandAd

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
                    auctionListener.demandAuctionSucceed(demandAd, success)
                },
                onDemandLoadFailed = { failure ->
                    auctionListener.demandAuctionFailed(demandAd, failure)
                },
                onAuctionFinished = {
                    auctionListener.winnerFound(demandAd, it)
                    auctionListener.auctionSucceed(demandAd, it.first())
                },
                onAuctionFailed = {
                    auctionsHolder.clearResults(demandAd)
                    auctionListener.auctionFailed(demandAd, it)
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