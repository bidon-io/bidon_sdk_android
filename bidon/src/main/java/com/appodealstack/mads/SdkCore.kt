package com.appodealstack.mads

import android.app.Activity
import android.os.Bundle
import com.appodealstack.mads.analytics.AnalyticsSource
import com.appodealstack.mads.analytics.AnalyticsSourceImpl
import com.appodealstack.mads.base.AdType
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.DemandsSource
import com.appodealstack.mads.demands.DemandsSourceImpl
import com.appodealstack.mads.demands.listeners.ListenersHolder
import com.appodealstack.mads.demands.listeners.ListenersHolderImpl

val SdkCore: BidOnCore by lazy {
    BidOnCoreImpl()
}

interface BidOnCore {
    fun loadAd(activity: Activity, adType: AdType, adParams: Bundle)
    fun setListener(adType: AdType, adListener: AdListener?)
    fun destroyAd(adType: AdType, bundle: Bundle)
    fun showAd(adType: AdType, bundle: Bundle)
    fun setExtras(bundle: Bundle)
    fun canShow(adType: AdType, adParams: Bundle): Boolean

    fun getListenerForDemand(adType: AdType): AdListener
}

internal class BidOnCoreImpl : BidOnCore,
    DemandsSource by DemandsSourceImpl(),
    AnalyticsSource by AnalyticsSourceImpl(),
    ListenersHolder by ListenersHolderImpl() {

    private var publisherListener: AdListener? = null

    override fun loadAd(activity: Activity, adType: AdType, adParams: Bundle) {
        demands.forEach { demand ->
            demand.loadAd(activity, adType, adParams)
        }
    }

    override fun setListener(adType: AdType, adListener: AdListener?) {
        publisherListener = adListener
    }

    override fun destroyAd(adType: AdType, bundle: Bundle) {
        demands.any { demand ->
            demand.destroyAd(adType, bundle)
        }
    }

    override fun showAd(adType: AdType, bundle: Bundle) {
        demands.any { demand ->
            demand.showAd(adType, bundle)
        }
    }

    override fun setExtras(bundle: Bundle) {
        demands.forEach { demand ->
            demand.setExtras(bundle)
        }
    }

    override fun canShow(adType: AdType, adParams: Bundle): Boolean {
        return demands.any { demand ->
            demand.canShow(adType, adParams)
        }
    }
}