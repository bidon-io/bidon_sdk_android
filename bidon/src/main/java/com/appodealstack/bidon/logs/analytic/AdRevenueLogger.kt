package com.appodealstack.bidon.logs.analytic

import com.appodealstack.bidon.ads.Ad

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface AdRevenueLogger {
    fun logAdRevenue(ad: Ad)
}
