package com.appodealstack.mads.analytics

internal interface AnalyticsSource {
    val analytics: List<Analytic>
    fun addAnalytics(analytics: Analytic)
}

internal class AnalyticsSourceImpl : AnalyticsSource {
    override val analytics = mutableListOf<Analytic>()

    override fun addAnalytics(analytics: Analytic) {
        this.analytics.add(analytics)
    }
}