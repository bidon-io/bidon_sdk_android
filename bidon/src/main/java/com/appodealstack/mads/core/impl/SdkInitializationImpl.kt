package com.appodealstack.mads.core.impl

import android.app.Activity
import com.appodealstack.mads.Core
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.SdkInitialization
import com.appodealstack.mads.analytics.Analytic
import com.appodealstack.mads.analytics.AnalyticParameters
import com.appodealstack.mads.core.*
import com.appodealstack.mads.core.ext.logInternal
import com.appodealstack.mads.demands.Adapter
import com.appodealstack.mads.demands.AdapterParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Suppress("UNCHECKED_CAST")
internal class SdkInitializationImpl : SdkInitialization {
    private val sdkCore: Core = SdkCore
    private val contextProvider = ContextProvider
    private val demands =
        mutableMapOf<Class<Adapter<AdapterParameters>>, Pair<Adapter<AdapterParameters>, AdapterParameters>>()
    private val analytics =
        mutableMapOf<Class<Analytic<AnalyticParameters>>, Pair<Analytic<AnalyticParameters>, AnalyticParameters>>()
    private val scope: CoroutineScope get() = CoroutineScope(Dispatchers.Default)

    override fun withContext(activity: Activity): SdkInitialization {
        ContextProvider.setContext(activity)
        return this
    }

    override fun registerAdapter(
        adapterClass: Class<out Adapter<*>>,
        parameters: AdapterParameters
    ): SdkInitialization {
        logInternal(Tag, "Creating instance for: $adapterClass")
        try {
            val instance = adapterClass.newInstance()
            demands[adapterClass as Class<Adapter<AdapterParameters>>] = (instance as Adapter<AdapterParameters>) to parameters
            logInternal(Tag, "Instance created: $instance")
        } catch (e: Exception) {
            logInternal(Tag, "Instance creating failed", e)
        }
        return this
    }

    override fun registerAnalytics(
        adapterClass: Class<out Analytic<*>>,
        parameters: AnalyticParameters
    ): SdkInitialization {
        logInternal(Tag, "Creating instance for analytics: $adapterClass")
        try {
            val instance = adapterClass.newInstance()
            analytics[adapterClass as Class<Analytic<AnalyticParameters>>] =
                (instance as Analytic<AnalyticParameters>) to parameters
            logInternal(Tag, "Analytics instance created: $instance")
        } catch (e: Exception) {
            logInternal(Tag, "Analytics instance creating failed", e)
        }
        return this
    }

    override suspend fun build(): InitializationResult {
        logInternal(Tag, "Demands: $demands")

        // Init Demands
        require(sdkCore is DemandsSource)
        demands.forEach { (_, pair) ->
            val (demand, params) = pair
            logInternal(Tag, "Demand is initializing: $demand")
            withTimeoutOrNull(InitializationTimeoutMs) {
                demand.init(
                    activity = requireNotNull(contextProvider.activity),
                    configParams = params
                )
                logInternal(Tag, "Demand is initialized: $demand")
                sdkCore.addDemands(demand)
            } ?: run {
                logInternal(Tag, "Demand not initialized: $demand")
            }
        }
        demands.clear()

        // Init Analytics
        require(sdkCore is AnalyticsSource)
        analytics.forEach { (_, pair) ->
            val analytic = pair.first
            val params = pair.second

            logInternal(Tag, "Analytics service is initializing: $analytic")
            withTimeoutOrNull(InitializationTimeoutMs) {
                analytic.init(
                    context = contextProvider.requiredContext,
                    configParams = params
                )
                logInternal(Tag, "Analytics service is initialized: $analytic")
                sdkCore.addAnalytics(analytic)
            } ?: run {
                logInternal(Tag, "Analytics service not initialized: $analytic")
            }
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
private const val Tag = "Initializer"