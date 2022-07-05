package com.appodealstack.mads.core.impl

import com.appodealstack.mads.analytics.Analytic
import com.appodealstack.mads.core.AnalyticsSource

internal class AnalyticsSourceImpl : AnalyticsSource {
    override val analytics = mutableListOf<Analytic>()

    override fun addAnalytics(analytics: Analytic) {
        this.analytics.add(analytics)
    }
}