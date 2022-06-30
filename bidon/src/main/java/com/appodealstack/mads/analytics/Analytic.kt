package com.appodealstack.mads.analytics

import android.content.Context
import android.os.Bundle
import com.appodealstack.mads.demands.DemandId

interface Analytic {
    val analyticsId: DemandId

    suspend fun init(context: Context, configParams: Bundle)

    fun logEvent(map: Map<String, Any>)
}