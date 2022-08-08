package com.appodealstack.bidon.core.impl

import com.appodealstack.bidon.analytics.Analytic
import com.appodealstack.bidon.analytics.AnalyticParameters
import com.appodealstack.bidon.core.AnalyticsSource

internal class AnalyticsSourceImpl : AnalyticsSource {
    override val analytics = mutableListOf<Analytic<AnalyticParameters>>()

    override fun addAnalytics(analytics: Analytic<AnalyticParameters>) {
        this.analytics.add(analytics)
    }
}