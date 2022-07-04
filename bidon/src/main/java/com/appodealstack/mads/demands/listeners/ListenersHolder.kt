package com.appodealstack.mads.demands.listeners

import com.appodealstack.mads.auctions.AuctionData
import com.appodealstack.mads.auctions.AuctionListener
import com.appodealstack.mads.base.AdType
import com.appodealstack.mads.base.ext.addOrRemoveNull
import com.appodealstack.mads.demands.AdListener

internal interface ListenersHolder {
    val auctionListener: AuctionListener

    fun addUserListener(adType: AdType, adListener: AdListener?)
    fun registerDemandListener(adType: AdType, adListener: AdListener)
    fun registerSdkCoreListener(adType: AdType, adListener: AdListener)

    fun getListenerForDemand(adType: AdType): AdListener
}

internal class ListenersHolderImpl : ListenersHolder {
    private val userListeners = mutableMapOf<AdType, AdListener>()

    override val auctionListener: AuctionListener by lazy {
        object : AuctionListener {
            override fun onDemandAdLoaded(adType: AdType, ad: AuctionData.Success) {
                userListeners[adType]?.onDemandAdLoaded(ad)
            }

            override fun onDemandAdLoadFailed(adType: AdType, ad: AuctionData.Failure) {
                userListeners[adType]?.onDemandAdLoadFailed(ad)
            }

            override fun onWinnerFound(adType: AdType, ads: List<AuctionData.Success>) {
                userListeners[adType]?.onWinnerFound(ads)
            }

            override fun onAdLoaded(adType: AdType, ad: AuctionData.Success) {
                userListeners[adType]?.onAdLoaded(ad)
            }

            override fun onAdLoadFailed(adType: AdType, cause: Throwable) {
                userListeners[adType]?.onAdLoadFailed(cause)
            }
        }
    }

    private val coreListeners: Map<AdType, AdListener> = AdType.values().mapNotNull { adType ->
        createAdListenerFor(adType)?.let { listener -> adType to listener }
    }.toMap()

    override fun addUserListener(adType: AdType, adListener: AdListener?) {
        userListeners.addOrRemoveNull(adType, adListener)
    }

    override fun registerDemandListener(adType: AdType, adListener: AdListener) {
    }

    override fun registerSdkCoreListener(adType: AdType, adListener: AdListener) {
    }

    override fun getListenerForDemand(adType: AdType): AdListener {
        return requireNotNull(coreListeners[adType]) {
            "Core AdCallback was not found for $adType"
        }
    }

    private fun createAdListenerFor(adType: AdType): AdListener? {
        return when (adType) {
            AdType.Interstitial -> object : AdListener {
                override fun onAdDisplayFailed(ad: AuctionData.Failure) {
                    userListeners[adType]?.onAdDisplayFailed(ad)
                }

                override fun onAdDisplayed(ad: AuctionData.Success) {
                    userListeners[adType]?.onAdDisplayed(ad)
                }

                override fun onAdClicked(ad: AuctionData.Success) {
                    userListeners[adType]?.onAdClicked(ad)
                }

                override fun onAdHidden(ad: AuctionData.Success) {
                    userListeners[adType]?.onAdHidden(ad)
                }

                override fun onAdLoaded(ad: AuctionData.Success) {
                    userListeners[adType]?.onAdLoaded(ad)
                }

                override fun onAdLoadFailed(cause: Throwable) {
                    userListeners[adType]?.onAdLoadFailed(cause)
                }

                override fun onDemandAdLoaded(ad: AuctionData.Success) {
                    userListeners[adType]?.onDemandAdLoaded(ad)
                }

                override fun onDemandAdLoadFailed(ad: AuctionData.Failure) {
                    userListeners[adType]?.onDemandAdLoadFailed(ad)
                }

                override fun onWinnerFound(ads: List<AuctionData.Success>) {
                    userListeners[adType]?.onWinnerFound(ads)
                }
            }
            AdType.Banner -> null
            AdType.Rewarded -> null
            AdType.Native -> null
        }
    }
}