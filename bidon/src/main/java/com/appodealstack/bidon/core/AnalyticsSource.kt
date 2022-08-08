package com.appodealstack.bidon.core

import com.appodealstack.bidon.analytics.Analytic
import com.appodealstack.bidon.analytics.AnalyticParameters

internal interface AnalyticsSource {
    val analytics: List<Analytic<AnalyticParameters>>
    fun addAnalytics(analytics: Analytic<AnalyticParameters>)
}

