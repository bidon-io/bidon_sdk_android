package com.appodealstack.bidon.analytics

import com.appodealstack.bidon.demands.Ad

interface AdRevenueLogger {
    fun logAdRevenue(mediationNetwork: BNMediationNetwork, ad: Ad)
}

interface MediationNetwork {
    val mediationNetwork: BNMediationNetwork
}