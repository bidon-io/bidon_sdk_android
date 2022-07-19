package com.appodealstack.appsflyer

import android.app.Application
import android.content.Context
import com.appodealstack.mads.analytics.Analytic
import com.appodealstack.mads.analytics.AnalyticParameters
import com.appodealstack.mads.analytics.BNMediationNetwork
import com.appodealstack.mads.analytics.RevenueLogger
import com.appodealstack.mads.core.ext.logInternal
import com.appodealstack.mads.demands.DemandId
import com.appsflyer.AppsFlyerLib
import com.appsflyer.adrevenue.AppsFlyerAdRevenue
import com.appsflyer.adrevenue.adnetworks.generic.MediationNetwork
import com.appsflyer.attribution.AppsFlyerRequestListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume

sealed interface AppsflyerParameters : AnalyticParameters {
    /**
     * [AppsFlyerLib] - will be initialized by BidOn SDK.
     * Provide [devKey].
     */
    data class DevKey(val devKey: String = AppsflyerDevKey) : AppsflyerParameters

    /**
     * [AppsFlyerLib] - initialized by publisher.
     */
    object JustRegister : AppsflyerParameters
}

private val AppsflyerDemandId = DemandId("appsflyer")
private const val AppsflyerDevKey = "XXXXXXXXXXXXX"

class AppsflyerAnalytics : Analytic<AppsflyerParameters>, RevenueLogger {
    override val analyticsId: DemandId = AppsflyerDemandId

    override suspend fun init(context: Context, configParams: AppsflyerParameters) {
        val afRevenueBuilder = AppsFlyerAdRevenue.Builder(context.applicationContext as Application)
        AppsFlyerAdRevenue.initialize(afRevenueBuilder.build())
        suspendCancellableCoroutine { continuation ->
            when (configParams) {
                is AppsflyerParameters.DevKey -> {
                    AppsFlyerLib.getInstance()
                        .start(context, configParams.devKey, object : AppsFlyerRequestListener {
                            override fun onSuccess() {
                                continuation.resume(Unit)
                            }

                            override fun onError(p0: Int, p1: String) {
                                logInternal("AppsflyerAdapter", "Error while Appsflyer initialization: $p0, $p1.")
                                continuation.resume(Unit)
                            }
                        })
                }
                AppsflyerParameters.JustRegister -> {
                    AppsFlyerLib.getInstance().start(context)
                    continuation.resume(Unit)
                }
            }
        }
    }

    override fun logAdRevenue(
        monetizationNetwork: String,
        mediationNetwork: BNMediationNetwork,
        eventRevenueCurrency: Currency,
        eventRevenue: Double,
        nonMandatory: Map<String, String>
    ) {
        AppsFlyerAdRevenue.logAdRevenue(
            monetizationNetwork,
            when (mediationNetwork) {
                BNMediationNetwork.IronSource -> MediationNetwork.ironsource
                BNMediationNetwork.ApplovinMax -> MediationNetwork.applovinmax
                BNMediationNetwork.GoogleAdmob -> MediationNetwork.googleadmob
                BNMediationNetwork.Mopub -> MediationNetwork.mopub
                BNMediationNetwork.Fyber -> MediationNetwork.fyber
                BNMediationNetwork.Appodeal -> MediationNetwork.appodeal
                BNMediationNetwork.Admost -> MediationNetwork.Admost
                BNMediationNetwork.Topon -> MediationNetwork.Topon
                BNMediationNetwork.Tradplus -> MediationNetwork.Tradplus
                BNMediationNetwork.Yandex -> MediationNetwork.Yandex
            },
            eventRevenueCurrency, eventRevenue, nonMandatory
        )
    }

}