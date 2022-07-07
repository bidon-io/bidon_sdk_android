package com.appodealstack.mads.core.impl

import android.content.Context
import com.appodealstack.mads.SdkInitialization
import com.appodealstack.mads.Core
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.analytics.Analytic
import com.appodealstack.mads.core.AnalyticsSource
import com.appodealstack.mads.core.ContextProvider
import com.appodealstack.mads.core.ext.logInternal
import com.appodealstack.mads.config.Configuration
import com.appodealstack.mads.config.BidonConfigurator
import com.appodealstack.mads.config.BidonConfiguratorInstance
import com.appodealstack.mads.config.StaticJsonConfiguration
import com.appodealstack.mads.demands.Adapter
import com.appodealstack.mads.core.DemandsSource
import com.appodealstack.mads.core.InitializationCallback
import com.appodealstack.mads.core.InitializationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class SdkInitializationImpl : SdkInitialization {
    private val sdkCore: Core = SdkCore
    private val contextProvider = ContextProvider
    private val demands = mutableMapOf<Class<out Adapter>, Adapter>()
    private val analytics = mutableMapOf<Class<out Analytic>, Analytic>()
    private val scope: CoroutineScope get() = CoroutineScope(Dispatchers.Default)

    private val bidonConfigurator: BidonConfigurator get() = BidonConfiguratorInstance

    override fun withContext(context: Context): SdkInitialization {
        ContextProvider.setContext(context)
        return this
    }

    override fun withConfigurations(vararg configurations: Configuration): SdkInitialization {
        bidonConfigurator.addConfigurations(*configurations)
        return this
    }

    override fun registerDemands(vararg adapterClasses: Class<out Adapter>): SdkInitialization {
        adapterClasses.forEach { demandClass ->
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

    override fun registerAnalytics(vararg analyticsClasses: Class<out Analytic>): SdkInitialization {
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
        registerPostBidConfiguration()
        logInternal("Demands", "Demands: $demands")

        // Init Demands
        require(sdkCore is DemandsSource)
        demands.forEach { (_, demand) ->
            logInternal("Demands", "Demand is initializing: $demand")
            demand.init(
                context = contextProvider.requiredContext,
                configParams = bidonConfigurator.getDemandConfig(demand.demandId)
            )
            logInternal("Demands", "Demand is initialized: $demand")
            sdkCore.addDemands(demand)
        }
        demands.clear()

        // Init Analytics
        require(sdkCore is AnalyticsSource)
        analytics.forEach { (_, analytics) ->
            analytics.init(
                context = contextProvider.requiredContext,
                configParams = bidonConfigurator.getServiceConfig(analytics.analyticsId)
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

    private fun registerPostBidConfiguration() {
        withConfigurations(
            StaticJsonConfiguration()
        )
    }
}