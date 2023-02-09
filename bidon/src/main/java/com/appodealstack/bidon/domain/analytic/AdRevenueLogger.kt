package com.appodealstack.bidon.domain.analytic

import com.appodealstack.bidon.domain.common.Ad

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface AdRevenueLogger {
    fun logAdRevenue(ad: Ad)
}
