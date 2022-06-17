package com.appodealstack.mads.analytics

interface Analytic {
    fun logEvent(map: Map<String, Any>)
}