package com.appodealstack.bidon.analytics

import com.appodealstack.bidon.adapters.Ad

interface AdRevenueLogger {
    fun logAdRevenue(ad: Ad)
}

interface MediationNetwork {
    val mediationNetwork: BNMediationNetwork
}