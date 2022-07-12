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
import com.appodealstack.mads.demands.AdapterParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal class SdkInitializationImpl : SdkInitialization {
    private val sdkCore: Core = SdkCore
    private val contextProvider = ContextProvider
    private val demands = mutableMapOf<Class<Adapter<AdapterParameters>>, Pair<Adapter<AdapterParameters>, AdapterParameters>>()
    private val analytics = mutableMapOf<Class<out Analytic>, Analytic>()
    private val scope: CoroutineScope get() = CoroutineScope(Dispatchers.Default)

    private val bidonConfigurator: BidonConfigurator get() = BidonConfiguratorInstance

    override fun withContext(context: Context): SdkInitialization {
        ContextProvider.setContext(context)
        return this
    }

    override fun registerAdapter(
        adapterClass: Class<out Adapter<*>>,
        parameters: AdapterParameters
    ): SdkInitialization {
        logInternal("Initializer", "Creating instance for: $adapterClass")
        try {
            val instance = adapterClass.newInstance()
            demands[adapterClass as Class<Adapter<AdapterParameters>>] = (instance as Adapter<AdapterParameters>) to parameters
            logInternal("Initializer", "Instance created: $instance")
        } catch (e: Exception) {
            logInternal("Initializer", "Instance creating failed", e)
        }
        return this
    }

    override suspend fun build(): InitializationResult {
        logInternal("Demands", "Demands: $demands")

        // Init Demands
        require(sdkCore is DemandsSource)
        demands.forEach { (_, pair) ->
            val (demand, params ) = pair
            logInternal("Demands", "Demand is initializing: $demand")
            withTimeoutOrNull(InitializationTimeoutMs) {
                demand.init(
                    context = contextProvider.requiredContext,
                    configParams = params
                )
                logInternal("Demands", "Demand is initialized: $demand")
                sdkCore.addDemands(demand)
            } ?: run {
                logInternal("Demands", "Demand not initialized: $demand")
            }
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
        sdkCore.isInitialized = true
        return InitializationResult.Success
    }

    override fun build(initCallback: InitializationCallback) {
        scope.launch {
            initCallback.onFinished(
                result = build()
            )
        }
    }
}

private const val InitializationTimeoutMs = 5000L