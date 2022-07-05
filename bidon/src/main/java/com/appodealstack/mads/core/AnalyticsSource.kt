package com.appodealstack.mads.core

import com.appodealstack.mads.analytics.Analytic

internal interface AnalyticsSource {
    val analytics: List<Analytic>
    fun addAnalytics(analytics: Analytic)
}

