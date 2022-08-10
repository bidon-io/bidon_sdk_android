package com.appodealstack.bidon.analytics

import com.appodealstack.bidon.adapters.Ad

fun interface AdRevenueInterceptor {
    fun onAdRevenueReceived(ad: Ad)
}