package com.appodealstack.mads.core

import com.appodealstack.mads.analytics.Analytic
import com.appodealstack.mads.analytics.AnalyticParameters

internal interface AnalyticsSource {
    val analytics: List<Analytic<AnalyticParameters>>
    fun addAnalytics(analytics: Analytic<AnalyticParameters>)
}

