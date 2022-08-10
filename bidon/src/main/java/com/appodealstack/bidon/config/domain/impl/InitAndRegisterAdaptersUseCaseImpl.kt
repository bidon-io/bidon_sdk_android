package com.appodealstack.bidon.config.domain.impl

import android.app.Activity
import com.appodealstack.bidon.config.data.models.ConfigResponse
import com.appodealstack.bidon.config.domain.InitAndRegisterAdaptersUseCase
import com.appodealstack.bidon.core.AdaptersSource
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInfo
import com.appodealstack.bidon.adapters.Adapter
import com.appodealstack.bidon.adapters.AdapterParameters
import com.appodealstack.bidon.adapters.Initializable
import kotlinx.coroutines.withTimeoutOrNull

@Suppress("UNCHECKED_CAST")
internal class InitAndRegisterAdaptersUseCaseImpl(
    private val adaptersSource: AdaptersSource
) : InitAndRegisterAdaptersUseCase {

    override suspend operator fun invoke(
        activity: Activity,
        notInitializedAdapters: List<Adapter>,
        configResponse: ConfigResponse
    ) {
        val timeout = configResponse.initializationTimeout
        val readyAdapters = notInitializedAdapters.mapNotNull { adapter ->
            val initializable = adapter as? Initializable<AdapterParameters>
                ?: return@mapNotNull run {
                    // Adapter is not Initializable. It's ready to use
                    adapter
                }

            val adapterParameters = configResponse.adapters
                .firstNotNullOfOrNull { (adapterName, json) ->
                    if (adapter.demandId.demandId == adapterName) {
                        try {
                            adapter.parseConfigParam(json)
                        } catch (e: Exception) {
                            logError(Tag, "Error while parsing AdapterParameters for ${adapter.demandId.demandId}: $json", e)
                            null
                        }
                    } else {
                        null
                    }
                }

            if (adapterParameters == null) {
                logError(Tag, "Config parameters is null. Adapter not initialized: $adapter")
                null
            } else {
                withTimeoutOrNull(timeout) {
                    initializable.init(
                        activity = activity,
                        configParams = adapterParameters
                    )
                    adapter
                } ?: run {
                    logError(Tag, "Adapter's initializing timed out. Adapter not initialized: $adapter")
                    null
                }
            }
        }
        logInfo(Tag, "Registered adapters: ${readyAdapters.joinToString { it::class.java.simpleName }}")
        adaptersSource.add(readyAdapters)
    }
}

private const val Tag = "Initializer"