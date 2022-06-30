package com.appodealstack.mads

import android.content.Context
import com.appodealstack.mads.analytics.Analytic
import com.appodealstack.mads.config.Configuration
import com.appodealstack.mads.demands.Demand
import com.appodealstack.mads.impl.BidOnInitializationImpl
import com.appodealstack.mads.initializing.InitializationCallback
import com.appodealstack.mads.initializing.InitializationResult

val BidOnInitializer: BidOnInitialization by lazy { BidOnInitializationImpl() }

interface BidOnInitialization {
    fun withContext(context: Context): BidOnInitialization
    fun withConfigurations(vararg configurations: Configuration): BidOnInitialization

    fun registerAnalytics(vararg analyticsClasses: Class<out Analytic>): BidOnInitialization
    fun registerDemands(vararg demandClasses: Class<out Demand>): BidOnInitialization

    suspend fun build(): InitializationResult
    fun build(initCallback: InitializationCallback)
}