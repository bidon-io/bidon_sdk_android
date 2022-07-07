package com.appodealstack.mads.core.impl

import android.app.Activity
import android.os.Bundle
import com.appodealstack.mads.Core
import com.appodealstack.mads.auctions.AdsRepository
import com.appodealstack.mads.auctions.AdsRepositoryImpl
import com.appodealstack.mads.auctions.NewAuction
import com.appodealstack.mads.core.AnalyticsSource
import com.appodealstack.mads.core.DemandsSource
import com.appodealstack.mads.core.ListenersHolder
import com.appodealstack.mads.core.ext.logInternal
import com.appodealstack.mads.core.ext.retrieveAuctionRequests
import com.appodealstack.mads.demands.*

internal class CoreImpl(
    private val adsRepository: AdsRepository = AdsRepositoryImpl()
) : Core,
    DemandsSource by DemandsSourceImpl(),
    AnalyticsSource by AnalyticsSourceImpl(),
    ListenersHolder by ListenersHolderImpl() {

    override fun loadAd(activity: Activity?, demandAd: DemandAd, adParams: Bundle) {
        if (!adsRepository.isAuctionActive(demandAd)) {
            val auction = NewAuction
            adsRepository.addAuction(demandAd, auction)
            auction.start(
                mediationRequests = adapters
                    .filterIsInstance<Adapter.Mediation>()
                    .retrieveAuctionRequests(activity, demandAd, adParams)
                    .toSet(),
                postBidRequests = adapters
                    .filterIsInstance<Adapter.PostBid>()
                    .retrieveAuctionRequests(activity, demandAd, adParams)
                    .toSet(),
                onDemandLoaded = { auctionResult ->
                    auctionListener.demandAuctionSucceed(auctionResult)
                },
                onDemandLoadFailed = { throwable ->
                    auctionListener.demandAuctionFailed(demandAd, throwable)
                },
                onAuctionFailed = {
                    adsRepository.clearResults(demandAd)
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
        return adsRepository.clearResults(demandAd)
    }

    override fun setExtras(demandAd: DemandAd, adParams: Bundle) {
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

}

private const val Tag = "SdkCore"