package com.appodealstack.bidon.core.impl

import android.app.Activity
import com.appodealstack.bidon.Core
import com.appodealstack.bidon.SdkCore
import com.appodealstack.bidon.SdkInitialization
import com.appodealstack.bidon.core.*
import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.demands.Adapter
import com.appodealstack.bidon.demands.AdapterParameters
import com.appodealstack.bidon.demands.Initializable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Suppress("UNCHECKED_CAST")
internal class SdkInitializationImpl : SdkInitialization {
    private val sdkCore: Core = SdkCore
    private val contextProvider = ContextProvider
    private val demands =
        mutableMapOf<Class<Adapter>, Pair<Adapter, AdapterParameters?>>()
    private val scope: CoroutineScope get() = CoroutineScope(Dispatchers.Default)

    override fun withContext(activity: Activity): SdkInitialization {
        ContextProvider.setContext(activity)
        return this
    }

    override fun registerAdapter(
        adapterClass: Class<out Adapter>,
        parameters: AdapterParameters?
    ): SdkInitialization {
        logInternal(Tag, "Creating instance for: $adapterClass")
        try {
            val instance = adapterClass.newInstance()
            demands[adapterClass as Class<Adapter>] = (instance as Adapter) to parameters
            logInternal(Tag, "Instance created: $instance")
        } catch (e: Exception) {
            logInternal(Tag, "Instance creating failed", e)
        }
        return this
    }

    override suspend fun build() {
        logInternal(Tag, "Demands: $demands")

        // Init Demands
        require(sdkCore is DemandsSource)
        demands.forEach { (_, pair) ->
            val (demand, params) = pair
            logInternal(Tag, "Demand is initializing: $demand")
            withTimeoutOrNull(InitializationTimeoutMs) {
                (demand as? Initializable<AdapterParameters>)?.init(
                    activity = requireNotNull(contextProvider.activity),
                    configParams = requireNotNull(params)
                )
                logInternal(Tag, "Demand is initialized: $demand")
                sdkCore.addDemands(demand)
            } ?: run {
                logInternal(Tag, "Demand not initialized: $demand")
            }
        }
        demands.clear()
        sdkCore.isInitialized = true
    }

    override fun build(initCallback: InitializationCallback) {
        scope.launch {
            build()
            initCallback.onFinished()
        }
    }
}

private const val InitializationTimeoutMs = 5000L
private const val Tag = "Initializer"