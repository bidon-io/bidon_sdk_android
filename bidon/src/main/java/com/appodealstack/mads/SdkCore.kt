package com.appodealstack.mads

import android.app.Activity
import android.os.Bundle
import com.appodealstack.mads.core.impl.CoreImpl
import com.appodealstack.mads.core.impl.ListenersHolderImpl
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.AdRevenueListener
import com.appodealstack.mads.demands.DemandAd

val SdkCore: Core by lazy { CoreImpl() }

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