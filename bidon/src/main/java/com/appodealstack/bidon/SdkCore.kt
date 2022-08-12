package com.appodealstack.bidon

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.appodealstack.bidon.adapters.Ad
import com.appodealstack.bidon.adapters.AdListener
import com.appodealstack.bidon.adapters.AdRevenueListener
import com.appodealstack.bidon.adapters.DemandAd
import com.appodealstack.bidon.adapters.banners.AutoRefresh
import com.appodealstack.bidon.analytics.AdRevenueInterceptor
import com.appodealstack.bidon.auctions.domain.AuctionResolver
import com.appodealstack.bidon.core.InitializationCallback
import com.appodealstack.bidon.core.impl.ListenersHolderImpl
import com.appodealstack.bidon.di.DI.initDependencyInjection
import com.appodealstack.bidon.di.get
import kotlinx.coroutines.flow.StateFlow

val SdkCore: Core by lazy {
    initDependencyInjection()
    get()
}

@Deprecated("")
interface Core {
    /**
     * Should be changed only in [SdkInitialization]
     */
    val isInitialized: Boolean
    val isInitializing: Boolean
    val sdkState: StateFlow<SdkState>

    fun init(
        activity: Activity,
        appKey: String,
        callback: InitializationCallback? = null
    )

    fun loadAd(activity: Activity?, demandAd: DemandAd)
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

    fun saveAuctionResolver(demandAd: DemandAd, auctionResolver: AuctionResolver)

    /**
     * implemented in [AutoRefresh]
     */
    fun setAutoRefresh(demandAd: DemandAd, autoRefresh: AutoRefresh)

    /**
     * implemented in [ListenersHolderImpl]
     */
    fun getListenerForDemand(demandAd: DemandAd): AdListener

    fun logAdRevenue(ad: Ad)
    fun getAdRevenueInterceptor(): AdRevenueInterceptor?

}

