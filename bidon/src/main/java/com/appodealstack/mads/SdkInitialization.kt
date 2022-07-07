package com.appodealstack.mads

import android.content.Context
import com.appodealstack.mads.analytics.Analytic
import com.appodealstack.mads.config.Configuration
import com.appodealstack.mads.core.InitializationCallback
import com.appodealstack.mads.core.InitializationResult
import com.appodealstack.mads.demands.Adapter
import com.appodealstack.mads.core.impl.SdkInitializationImpl

val BidOnInitializer: SdkInitialization by lazy { SdkInitializationImpl() }

interface SdkInitialization {
    fun withContext(context: Context): SdkInitialization
    fun withConfigurations(vararg configurations: Configuration): SdkInitialization

    fun registerAnalytics(vararg analyticsClasses: Class<out Analytic>): SdkInitialization
    fun registerDemands(vararg adapterClasses: Class<out Adapter>): SdkInitialization

    suspend fun build(): InitializationResult
    fun build(initCallback: InitializationCallback)
}