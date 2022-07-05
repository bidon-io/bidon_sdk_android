package com.appodealstack.mads.core.impl

import com.appodealstack.mads.auctions.AuctionData
import com.appodealstack.mads.auctions.AuctionListener
import com.appodealstack.mads.demands.AdType
import com.appodealstack.mads.core.ListenersHolder
import com.appodealstack.mads.core.ext.addOrRemoveIfNull
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.DemandAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class ListenersHolderImpl : ListenersHolder {
    private val userListeners = mutableMapOf<DemandAd, AdListener>()
    private val mainScope by lazy { CoroutineScope(Dispatchers.Main) }

    override val auctionListener: AuctionListener by lazy {
        object : AuctionListener {
            override fun demandAuctionSucceed(demandAd: DemandAd, ad: AuctionData.Success) {
                mainScope.launch {
                    userListeners[demandAd]?.onDemandAdLoaded(ad)
                }
            }

            override fun demandAuctionFailed(demandAd: DemandAd, ad: AuctionData.Failure) {
                mainScope.launch {
                    userListeners[demandAd]?.onDemandAdLoadFailed(ad)
                }
            }

            override fun winnerFound(demandAd: DemandAd, ads: List<AuctionData.Success>) {
                mainScope.launch {
                    userListeners[demandAd]?.onWinnerFound(ads)
                }
            }

            override fun auctionSucceed(demandAd: DemandAd, ad: AuctionData.Success) {
                mainScope.launch {
                    userListeners[demandAd]?.onAdLoaded(ad)
                }
            }

            override fun auctionFailed(demandAd: DemandAd, cause: Throwable) {
                mainScope.launch {
                    userListeners[demandAd]?.onAdLoadFailed(cause)
                }
            }
        }
    }

    override fun addUserListener(demandAd: DemandAd, adListener: AdListener?) {
        userListeners.addOrRemoveIfNull(demandAd, adListener)
    }

    override fun getListenerForDemand(demandAd: DemandAd): AdListener = when (demandAd.adType) {
        AdType.Interstitial -> object : AdListener {
            override fun onAdDisplayFailed(ad: AuctionData.Failure) {
                mainScope.launch {
                    userListeners[demandAd]?.onAdDisplayFailed(ad)
                }
            }

            override fun onAdDisplayed(ad: AuctionData.Success) {
                mainScope.launch {
                    userListeners[demandAd]?.onAdDisplayed(ad)
                }
            }

            override fun onAdClicked(ad: AuctionData.Success) {
                mainScope.launch {
                    userListeners[demandAd]?.onAdClicked(ad)
                }
            }

            override fun onAdHidden(ad: AuctionData.Success) {
                mainScope.launch {
                    userListeners[demandAd]?.onAdHidden(ad)
                }
            }

            /** Next callbacks implemented in [auctionListener] */
            override fun onAdLoaded(ad: AuctionData.Success) {}
            override fun onAdLoadFailed(cause: Throwable) {}
            override fun onDemandAdLoaded(ad: AuctionData.Success) {}
            override fun onDemandAdLoadFailed(ad: AuctionData.Failure) {}
            override fun onWinnerFound(ads: List<AuctionData.Success>) {}
        }
        AdType.Banner,
        AdType.Rewarded,
        AdType.Native -> TODO("Not implemented")
    }
}