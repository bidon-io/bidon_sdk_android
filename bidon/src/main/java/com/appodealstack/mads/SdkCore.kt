package com.appodealstack.mads

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.appodealstack.mads.analytics.BNMediationNetwork
import com.appodealstack.mads.core.impl.CoreImpl
import com.appodealstack.mads.core.impl.ListenersHolderImpl
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.AdRevenueListener
import com.appodealstack.mads.demands.DemandAd
import com.appodealstack.mads.demands.banners.AutoRefresh
import java.util.*

val SdkCore: Core by lazy { CoreImpl() }

interface Core {
    /**
     * Should be changed only in [SdkInitialization]
     */
    var isInitialized: Boolean

    fun loadAd(activity: Activity?, demandAd: DemandAd, adParams: Bundle)
    fun loadAdView(
        context: Context,
        demandAd: DemandAd,
        adParams: Bundle,
        autoRefresh: AutoRefresh,
        onViewReady: (View) -> Unit,
        adContainer: ViewGroup? = null,
    )

    fun showAd(activity: Activity?, demandAd: DemandAd, adParams: Bundle)

    fun canShow(demandAd: DemandAd): Boolean
    fun destroyAd(demandAd: DemandAd, adParams: Bundle)
    fun setExtras(demandAd: DemandAd, adParams: Bundle)
    fun setListener(demandAd: DemandAd, adListener: AdListener)
    fun setRevenueListener(demandAd: DemandAd, adRevenueListener: AdRevenueListener)

    fun getPlacement(demandAd: DemandAd): String?
    fun setPlacement(demandAd: DemandAd, placement: String?)

    /**
     * implemented in [AutoRefresherImpl]
     */
    fun setAutoRefresh(demandAd: DemandAd, autoRefresh: AutoRefresh)

    /**
     * implemented in [ListenersHolderImpl]
     */
    fun getListenerForDemand(demandAd: DemandAd): AdListener

    fun logAdRevenue(
        monetizationNetwork: String,
        mediationNetwork: BNMediationNetwork,
        eventRevenueCurrency: Currency,
        eventRevenue: Double,
        nonMandatory: Map<String, String>
    )
}