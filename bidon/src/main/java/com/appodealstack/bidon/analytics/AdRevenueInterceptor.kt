package com.appodealstack.bidon.analytics

import com.appodealstack.bidon.demands.Ad

fun interface AdRevenueInterceptor {
    fun onAdRevenueReceived(ad: Ad)
}