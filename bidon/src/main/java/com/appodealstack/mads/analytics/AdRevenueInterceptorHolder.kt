package com.appodealstack.mads.analytics

import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.core.ext.logInternal

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