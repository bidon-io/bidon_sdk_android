package com.appodealstack.mads.analytics

import android.content.Context
import com.appodealstack.mads.demands.DemandId
import java.util.*

interface AnalyticParameters

interface Analytic<T : AnalyticParameters> {
    val analyticsId: DemandId

    suspend fun init(context: Context, configParams: T)
}


interface RevenueLogger {
    fun logAdRevenue(
        monetizationNetwork: String,
        mediationNetwork: BNMediationNetwork,
        eventRevenueCurrency: Currency,
        eventRevenue: Double,
        nonMandatory: Map<String, String>
    )
}

enum class BNMediationNetwork {
    IronSource,
    ApplovinMax,
    GoogleAdmob,
    Mopub,
    Fyber,
    Appodeal,
    Admost,
    Topon,
    Tradplus,
    Yandex;
}