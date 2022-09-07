package com.appodealstack.bidon.domain.analytic

import com.appodealstack.bidon.domain.common.Ad

interface AdRevenueLogger {
    fun logAdRevenue(ad: Ad)
}
