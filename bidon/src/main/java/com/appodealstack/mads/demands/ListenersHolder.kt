package com.appodealstack.mads.demands

import com.appodealstack.mads.auctions.AuctionData
import com.appodealstack.mads.auctions.AuctionListener
import com.appodealstack.mads.base.AdType
import com.appodealstack.mads.base.ext.addOrRemoveIfNull

internal interface ListenersHolder {
    val auctionListener: AuctionListener

    fun addUserListener(demandAd: DemandAd, adListener: AdListener?)
    fun getListenerForDemand(demandAd: DemandAd): AdListener
}

internal class ListenersHolderImpl : ListenersHolder {
    private val userListeners = mutableMapOf<DemandAd, AdListener>()

    override val auctionListener: AuctionListener by lazy {
        object : AuctionListener {
            override fun onDemandAdLoaded(demandAd: DemandAd, ad: AuctionData.Success) {
                userListeners[demandAd]?.onDemandAdLoaded(ad)
            }

            override fun onDemandAdLoadFailed(demandAd: DemandAd, ad: AuctionData.Failure) {
                userListeners[demandAd]?.onDemandAdLoadFailed(ad)
            }

            override fun onWinnerFound(demandAd: DemandAd, ads: List<AuctionData.Success>) {
                userListeners[demandAd]?.onWinnerFound(ads)
            }

            override fun onAdLoaded(demandAd: DemandAd, ad: AuctionData.Success) {
                userListeners[demandAd]?.onAdLoaded(ad)
            }

            override fun onAdLoadFailed(demandAd: DemandAd, cause: Throwable) {
                userListeners[demandAd]?.onAdLoadFailed(cause)
            }
        }
    }

    override fun addUserListener(demandAd: DemandAd, adListener: AdListener?) {
        userListeners.addOrRemoveIfNull(demandAd, adListener)
    }

    override fun getListenerForDemand(demandAd: DemandAd): AdListener = when (demandAd.adType) {
        AdType.Interstitial -> object : AdListener {
            override fun onAdDisplayFailed(ad: AuctionData.Failure) {
                userListeners[demandAd]?.onAdDisplayFailed(ad)
            }

            override fun onAdDisplayed(ad: AuctionData.Success) {
                userListeners[demandAd]?.onAdDisplayed(ad)
            }

            override fun onAdClicked(ad: AuctionData.Success) {
                userListeners[demandAd]?.onAdClicked(ad)
            }

            override fun onAdHidden(ad: AuctionData.Success) {
                userListeners[demandAd]?.onAdHidden(ad)
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