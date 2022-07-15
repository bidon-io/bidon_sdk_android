package com.appodealstack.mads

import android.app.Activity
import android.content.Context
import com.appodealstack.mads.analytics.Analytic
import com.appodealstack.mads.config.Configuration
import com.appodealstack.mads.core.InitializationCallback
import com.appodealstack.mads.core.InitializationResult
import com.appodealstack.mads.demands.Adapter
import com.appodealstack.mads.core.impl.SdkInitializationImpl
import com.appodealstack.mads.demands.AdapterParameters

val BidOnInitializer: SdkInitialization by lazy { SdkInitializationImpl() }

interface SdkInitialization {
    fun withContext(activity: Activity): SdkInitialization

    fun registerAdapter(
        adapterClass: Class<out Adapter<*>>,
        parameters: AdapterParameters
    ): SdkInitialization

    suspend fun build(): InitializationResult
    fun build(initCallback: InitializationCallback)
}