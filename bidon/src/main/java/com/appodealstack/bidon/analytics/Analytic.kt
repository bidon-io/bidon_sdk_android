package com.appodealstack.bidon.analytics

import android.content.Context
import com.appodealstack.bidon.demands.Ad
import com.appodealstack.bidon.demands.DemandId

interface AnalyticParameters

interface Analytic<T : AnalyticParameters> {
    val analyticsId: DemandId

    suspend fun init(context: Context, configParams: T)
}

interface AdRevenueLogger {
    fun logAdRevenue(mediationNetwork: BNMediationNetwork, ad: Ad)
}