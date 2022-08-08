package com.appodealstack.bidon.analytics

import com.appodealstack.bidon.SdkCore

internal interface AdRevenueInterceptorHolder {
    fun obtainAdRevenueInterceptor(): AdRevenueInterceptor
}

internal class AdRevenueInterceptorHolderImpl : AdRevenueInterceptorHolder {
    private val adRevenueInterceptor by lazy {
        AdRevenueInterceptor { ad ->
            SdkCore.logAdRevenue(ad)
        }
    }

    override fun obtainAdRevenueInterceptor(): AdRevenueInterceptor {
        return adRevenueInterceptor
    }
}