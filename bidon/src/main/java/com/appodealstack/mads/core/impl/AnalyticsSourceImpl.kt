package com.appodealstack.mads.core.impl

import com.appodealstack.mads.analytics.Analytic
import com.appodealstack.mads.analytics.AnalyticParameters
import com.appodealstack.mads.core.AnalyticsSource

internal class AnalyticsSourceImpl : AnalyticsSource {
    override val analytics = mutableListOf<Analytic<AnalyticParameters>>()

    override fun addAnalytics(analytics: Analytic<AnalyticParameters>) {
        this.analytics.add(analytics)
    }
}