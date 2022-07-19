package com.appodealstack.mads.analytics

import com.appodealstack.mads.demands.Ad

fun interface AdRevenueInterceptor {
    fun onAdRevenueReceived(ad: Ad)
}