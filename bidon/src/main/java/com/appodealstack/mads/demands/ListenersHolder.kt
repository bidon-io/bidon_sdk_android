package com.appodealstack.mads.demands

import com.appodealstack.mads.auctions.AuctionData
import com.appodealstack.mads.auctions.AuctionListener
import com.appodealstack.mads.base.AdType
import com.appodealstack.mads.base.ext.addOrRemoveIfNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal interface ListenersHolder {
    val auctionListener: AuctionListener

    fun addUserListener(demandAd: DemandAd, adListener: AdListener?)
    fun getListenerForDemand(demandAd: DemandAd): AdListener
}

internal class ListenersHolderImpl : ListenersHolder {
    private val userListeners = mutableMapOf<DemandAd, AdListener>()
    private val mainScope by lazy { CoroutineScope(Dispatchers.Main) }

    override val auctionListener: AuctionListener by lazy {
        object : AuctionListener {
            override fun onDemandAdLoaded(demandAd: DemandAd, ad: AuctionData.Success) {
                mainScope.launch {
                    userListeners[demandAd]?.onDemandAdLoaded(ad)
                }
            }

            override fun onDemandAdLoadFailed(demandAd: DemandAd, ad: AuctionData.Failure) {
                mainScope.launch {
                    userListeners[demandAd]?.onDemandAdLoadFailed(ad)
                }
            }

            override fun onWinnerFound(demandAd: DemandAd, ads: List<AuctionData.Success>) {
                mainScope.launch {
                    userListeners[demandAd]?.onWinnerFound(ads)
                }
            }

            override fun onAdLoaded(demandAd: DemandAd, ad: AuctionData.Success) {
                mainScope.launch {
                    userListeners[demandAd]?.onAdLoaded(ad)
                }
            }

            override fun onAdLoadFailed(demandAd: DemandAd, cause: Throwable) {
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