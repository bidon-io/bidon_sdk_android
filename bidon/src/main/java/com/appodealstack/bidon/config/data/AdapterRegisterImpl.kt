package com.appodealstack.bidon.config.data

import android.app.Activity
import com.appodealstack.bidon.Core
import com.appodealstack.bidon.SdkCore
import com.appodealstack.bidon.config.domain.AdapterRegister
import com.appodealstack.bidon.core.ContextProvider
import com.appodealstack.bidon.core.DemandsSource
import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.demands.Adapter
import com.appodealstack.bidon.demands.AdapterParameters
import com.appodealstack.bidon.demands.Initializable
import kotlinx.coroutines.withTimeoutOrNull

@Suppress("UNCHECKED_CAST")
internal class AdapterRegisterImpl : AdapterRegister {
    private val sdkCore: Core = SdkCore
    private val contextProvider = ContextProvider
    private val demands =
        mutableMapOf<Class<Adapter>, Pair<Adapter, AdapterParameters?>>()
    override fun withContext(activity: Activity): AdapterRegister {
        ContextProvider.setContext(activity)
        return this
    }

    override fun registerAdapter(
        adapterClass: Class<out Adapter>,
        parameters: AdapterParameters?
    ): AdapterRegister {
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
        logInternal(Tag, "Adapters: $demands")

        // Init Demands
        require(sdkCore is DemandsSource)
        demands.forEach { (_, pair) ->
            val (demand, params) = pair
            logInternal(Tag, "Adapter is initializing: $demand")
            withTimeoutOrNull(InitializationTimeoutMs) {
                (demand as? Initializable<AdapterParameters>)?.init(
                    activity = requireNotNull(contextProvider.activity),
                    configParams = requireNotNull(params)
                )
                logInternal(Tag, "Adapter is initialized: $demand")
                sdkCore.addDemands(demand)
            } ?: run {
                logInternal(Tag, "Adapter not initialized: $demand")
            }
        }
        demands.clear()
        sdkCore.isInitialized = true
    }
}

private const val InitializationTimeoutMs = 5000L
private const val Tag = "Initializer"