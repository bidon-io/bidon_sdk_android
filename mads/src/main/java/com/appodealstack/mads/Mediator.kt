package com.appodealstack.mads

import android.content.Context
import com.appodealstack.mads.analytics.Analytic
import com.appodealstack.mads.config.Configuration
import com.appodealstack.mads.demands.Demand
import com.appodealstack.mads.impl.MadsCore
import com.appodealstack.mads.initializing.InitializationCallback
import com.appodealstack.mads.initializing.InitializationResult

object Mads: Mediator by MadsCore

interface Mediator {
    fun withContext(context: Context): Mediator
    fun withConfigurations(vararg configurations: Configuration): Mediator

    fun registerAnalytics(vararg analytics: Analytic): Mediator
    fun registerDemands(vararg demandClasses: Class<out Demand>): Mediator

    suspend fun build(): InitializationResult
    fun build(initCallback: InitializationCallback)
}