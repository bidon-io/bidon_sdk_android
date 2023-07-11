package org.bidon.sdk.config.impl

import android.content.Context
import kotlinx.coroutines.*
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.config.models.ConfigResponse
import org.bidon.sdk.config.usecases.InitAndRegisterAdaptersUseCase
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import kotlin.system.measureTimeMillis

/**
 * Created by Bidon Team on 06/02/2023.
 */
@Suppress("UNCHECKED_CAST")
internal class InitAndRegisterAdaptersUseCaseImpl(
    private val adaptersSource: AdaptersSource
) : InitAndRegisterAdaptersUseCase {

    override suspend operator fun invoke(
        context: Context,
        adapters: List<Adapter>,
        configResponse: ConfigResponse
    ) = coroutineScope {
        val deferredList = adapters.associate { adapter ->
            val demandId = adapter.demandId
            demandId to async {
                runCatching {
                    withTimeout(configResponse.initializationTimeout) {
                        val initializable = adapter as? Initializable<AdapterParameters>
                        if (initializable == null) {
                            adapter
                        } else {
                            val timeStart = measureTimeMillis {
                                val adapterParameters =
                                    parseAdapterParameters(configResponse, adapter).getOrThrow()
                                adapter.init(context, adapterParameters)
                            }
                            logInfo(
                                Tag,
                                "Adapter ${demandId.demandId} initialized in $timeStart ms."
                            )
                            adapter
                        }
                    }
                }
            }
        }
        val readyAdapters = deferredList.mapNotNull { (demandId, deferred) ->
            deferred.await().onFailure { cause ->
                logError(Tag, "Adapter not initialized: ${demandId.demandId}", cause)
            }.getOrNull()
        }
        logInfo(
            Tag,
            "Registered adapters: ${readyAdapters.joinToString { it::class.java.simpleName }}"
        )
        adaptersSource.add(readyAdapters)
    }

    private fun parseAdapterParameters(
        configResponse: ConfigResponse,
        adapter: Initializable<AdapterParameters>
    ): Result<AdapterParameters> = runCatching {
        val json = configResponse.adapters[(adapter as Adapter).demandId.demandId]
        requireNotNull(json) {
            "No config found for Adapter($adapter). Adapter not initialized."
        }
        adapter.parseConfigParam(json.toString())
    }
}

private const val Tag = "InitAndRegisterUserCase"
