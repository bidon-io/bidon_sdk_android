package com.appodealstack.mads.core.impl

import com.appodealstack.mads.auctions.AuctionListener
import com.appodealstack.mads.auctions.AuctionResult
import com.appodealstack.mads.core.ListenersHolder
import com.appodealstack.mads.core.ext.addOrRemoveIfNull
import com.appodealstack.mads.demands.Ad
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.AdType
import com.appodealstack.mads.demands.DemandAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class ListenersHolderImpl : ListenersHolder {
    private val userListeners = mutableMapOf<DemandAd, AdListener>()
    private val mainScope by lazy { CoroutineScope(Dispatchers.Main) }

    override val auctionListener: AuctionListener by lazy {
        object : AuctionListener {
            override fun demandAuctionSucceed(auctionResult: AuctionResult) {
                mainScope.launch {
                    userListeners[auctionResult.ad.demandAd]?.onDemandAdLoaded(auctionResult.ad)
                }
            }

            override fun demandAuctionFailed(demandAd: DemandAd, error: Throwable) {
                mainScope.launch {
                    userListeners[demandAd]?.onDemandAdLoadFailed(error)
                }
            }

            override fun auctionSucceed(demandAd: DemandAd, results: List<AuctionResult>) {
                mainScope.launch {
                    userListeners[demandAd]?.onAuctionFinished(results.map { it.ad })
                }
            }

            override fun auctionFailed(demandAd: DemandAd, cause: Throwable) {
                mainScope.launch {
                    userListeners[demandAd]?.onAdLoadFailed(cause)
                }
            }

            override fun winnerFound(winner: AuctionResult) {
                mainScope.launch {
                    userListeners[winner.ad.demandAd]?.onAdLoaded(winner.ad)
                }
            }
        }
    }

    override fun addUserListener(demandAd: DemandAd, adListener: AdListener?) {
        userListeners.addOrRemoveIfNull(demandAd, adListener)
    }

    override fun getListenerForDemand(demandAd: DemandAd): AdListener = when (demandAd.adType) {
        AdType.Interstitial -> object : AdListener {
            override fun onAdDisplayFailed(cause: Throwable) {
                mainScope.launch {
                    userListeners[demandAd]?.onAdDisplayFailed(cause)
                }
            }

            override fun onAdDisplayed(ad: Ad) {
                mainScope.launch {
                    userListeners[demandAd]?.onAdDisplayed(ad)
                }
            }

            override fun onAdClicked(ad: Ad) {
                mainScope.launch {
                    userListeners[demandAd]?.onAdClicked(ad)
                }
            }

            override fun onAdHidden(ad: Ad) {
                mainScope.launch {
                    userListeners[demandAd]?.onAdHidden(ad)
                }
            }

            /** Next callbacks implemented in [auctionListener] */
            override fun onAdLoaded(ad: Ad) {}
            override fun onAdLoadFailed(cause: Throwable) {}
            override fun onDemandAdLoaded(ad: Ad) {}
            override fun onDemandAdLoadFailed(cause: Throwable) {}
            override fun onAuctionFinished(ads: List<Ad>) {}
        }
        AdType.Banner,
        AdType.Rewarded -> TODO("Not implemented")
    }
}