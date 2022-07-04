package com.appodealstack.mads.impl

import android.annotation.SuppressLint
import android.content.Context
import com.appodealstack.mads.BidOnInitialization
import com.appodealstack.mads.Core
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.analytics.Analytic
import com.appodealstack.mads.analytics.AnalyticsSource
import com.appodealstack.mads.base.ContextProvider
import com.appodealstack.mads.base.ext.logInternal
import com.appodealstack.mads.config.Configuration
import com.appodealstack.mads.config.MadsConfigurator
import com.appodealstack.mads.config.MadsConfiguratorInstance
import com.appodealstack.mads.config.StaticJsonConfiguration
import com.appodealstack.mads.demands.Demand
import com.appodealstack.mads.demands.DemandsSource
import com.appodealstack.mads.initializing.InitializationCallback
import com.appodealstack.mads.initializing.InitializationResult
import com.appodealstack.mads.postbid.BidMachineDemand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
internal class BidOnInitializationImpl : BidOnInitialization {
    private val sdkCore: Core = SdkCore
    private val contextProvider = ContextProvider
    private val demands = mutableMapOf<Class<out Demand>, Demand>()
    private val analytics = mutableMapOf<Class<out Analytic>, Analytic>()
    private val scope: CoroutineScope get() = CoroutineScope(Dispatchers.Default)
    private val requiredContext: Context
        get() = contextProvider.requiredContext

    private val madsConfigurator: MadsConfigurator get() = MadsConfiguratorInstance

    override fun withContext(context: Context): BidOnInitialization {
        ContextProvider.setContext(context)
        return this
    }

    override fun withConfigurations(vararg configurations: Configuration): BidOnInitialization {
        madsConfigurator.addConfigurations(*configurations)
        return this
    }

    override fun registerDemands(vararg demandClasses: Class<out Demand>): BidOnInitialization {
        demandClasses.forEach { demandClass ->
            logInternal("Initializer", "Creating instance for: $demandClass")
            try {
                val instance = demandClass.newInstance()
                demands[demandClass] = instance
                logInternal("Initializer", "Instance created: $instance")
            } catch (e: Exception) {
                logInternal("Initializer", "Instance creating failed", e)
            }
        }
        return this
    }

    override fun registerAnalytics(vararg analyticsClasses: Class<out Analytic>): BidOnInitialization {
        analyticsClasses.forEach { analyticsClass ->
            try {
                val instance = analyticsClass.newInstance()
                analytics[analyticsClass] = instance
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return this
    }

    override suspend fun build(): InitializationResult {
        registerPostBidDemands()
        logInternal("Demands", "Demands: $demands")

        // Init Demands
        require(sdkCore is DemandsSource)
        demands.forEach { (_, demand) ->
            logInternal("Demands", "Demand is initializing: $demand")
            demand.init(
                context = requiredContext,
                configParams = madsConfigurator.getDemandConfig(demand.demandId)
            )
            logInternal("Demands", "Demand is initialized: $demand")
            sdkCore.addDemands(demand)
        }
        demands.clear()

        // Init Analytics
        require(sdkCore is AnalyticsSource)
        analytics.forEach { (_, analytics) ->
            analytics.init(
                context = requiredContext,
                configParams = madsConfigurator.getServiceConfig(analytics.analyticsId)
            )
            sdkCore.addAnalytics(analytics)
        }
        analytics.clear()
        return InitializationResult.Success
    }

    override fun build(initCallback: InitializationCallback) {
        scope.launch {
            initCallback.onFinished(
                result = build()
            )
        }
    }

    private fun registerPostBidDemands() {
        withConfigurations(
            StaticJsonConfiguration()
        )
        registerDemands(
            BidMachineDemand::class.java
        )
    }
}