package com.appodealstack.mads

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import com.appodealstack.mads.core.impl.CoreImpl
import com.appodealstack.mads.core.impl.ListenersHolderImpl
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.AdRevenueListener
import com.appodealstack.mads.demands.DemandAd

val SdkCore: Core by lazy { CoreImpl() }

interface Core {
    fun loadAd(activity: Activity?, demandAd: DemandAd, adParams: Bundle)
    fun loadAd(context: Context, demandAd: DemandAd, adParams: Bundle, onViewReady: (View) -> Unit)
    fun showAd(activity: Activity?, demandAd: DemandAd, adParams: Bundle)

    fun canShow(demandAd: DemandAd): Boolean
    fun destroyAd(demandAd: DemandAd, adParams: Bundle)
    fun setExtras(demandAd: DemandAd, adParams: Bundle)
    fun setListener(demandAd: DemandAd, adListener: AdListener)
    fun setRevenueListener(demandAd: DemandAd, adRevenueListener: AdRevenueListener)

    fun getPlacement(): String?
    fun setPlacement(placement: String?)
    fun setAutoRefresh(demandAd: DemandAd, autoRefresh: Boolean)

    /**
     * implemented at [ListenersHolderImpl]
     */
    fun getListenerForDemand(demandAd: DemandAd): AdListener
}