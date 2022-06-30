package com.appodealstack.mads.demands.listeners

import com.appodealstack.mads.base.AdType
import com.appodealstack.mads.base.ext.addOrRemoveNull
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.BidOnAd
import com.appodealstack.mads.demands.DemandError
import com.appodealstack.mads.demands.DemandId

internal interface ListenersHolder {
    fun addUserListener(adType: AdType, adListener: AdListener?)
    fun registerDemandListener(adType: AdType, adListener: AdListener)
    fun registerSdkCoreListener(adType: AdType, adListener: AdListener)

    fun getListenerForDemand(adType: AdType): AdListener
}

internal class ListenersHolderImpl : ListenersHolder {
    private val userListeners = mutableMapOf<AdType, AdListener>()

    private val coreListeners: Map<AdType, AdListener> = AdType.values().mapNotNull { adType ->
        createAdListenerFor(adType)?.let { listener -> adType to listener }
    }.toMap()

    override fun addUserListener(adType: AdType, adListener: AdListener?) {
        userListeners.addOrRemoveNull(adType, adListener)
    }

    override fun registerDemandListener(adType: AdType, adListener: AdListener) {
        TODO("Not yet implemented")
    }

    override fun registerSdkCoreListener(adType: AdType, adListener: AdListener) {
        TODO("Not yet implemented")
    }

    override fun getListenerForDemand(adType: AdType): AdListener {
        return requireNotNull(coreListeners[adType]) {
            "Core AdCallback was not found for $adType"
        }
    }

    private fun createAdListenerFor(adType: AdType): AdListener? {
        return when (adType) {
            AdType.Interstitial -> object : AdListener {
                override fun onAdLoaded(ad: BidOnAd?) {
                }

                override fun onAdLoadFailed(adUnitId: String?, error: DemandError?) {
                }

                override fun onDemandAdLoaded(demandId: DemandId, ad: BidOnAd) {
                    super.onDemandAdLoaded(demandId, ad)
                }

                override fun onDemandAdLoadFailed(demandId: DemandId, error: DemandError?) {
                    super.onDemandAdLoadFailed(demandId, error)
                }

                override fun onWinnerSelected(ad: BidOnAd) {
                    super.onWinnerSelected(ad)
                }

                override fun onAdDisplayed(ad: BidOnAd?) {
                    userListeners[adType]?.onAdDisplayed(ad)
                }

                override fun onAdDisplayFailed(ad: BidOnAd?, error: DemandError?) {
                    userListeners[adType]?.onAdDisplayFailed(ad, error)
                }

                override fun onAdClicked(ad: BidOnAd?) {
                    userListeners[adType]?.onAdClicked(ad)
                }

                override fun onAdHidden(ad: BidOnAd?) {
                    userListeners[adType]?.onAdHidden(ad)
                }
            }
            AdType.Banner -> null
            AdType.Rewarded -> null
            AdType.Native -> null
        }
    }
}