package com.appodealstack.mads.analytics

import android.content.Context
import com.appodealstack.mads.demands.Ad
import com.appodealstack.mads.demands.DemandId
import java.util.*

interface AnalyticParameters

interface Analytic<T : AnalyticParameters> {
    val analyticsId: DemandId

    suspend fun init(context: Context, configParams: T)
}

interface AdRevenueLogger {
    fun logAdRevenue(mediationNetwork: BNMediationNetwork, ad: Ad)
}