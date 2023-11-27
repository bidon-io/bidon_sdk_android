package org.bidon.sdk.config.impl

import android.content.Context
import kotlinx.coroutines.*
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.adapter.SupportsTestMode
import org.bidon.sdk.config.models.ConfigResponse
import org.bidon.sdk.config.usecases.InitAndRegisterAdaptersUseCase
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.SdkDispatchers
import kotlin.system.measureTimeMillis

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
@Suppress("UNCHECKED_CAST")
internal class InitAndRegisterAdaptersUseCaseImpl(
    private val adaptersSource: AdaptersSource
) : InitAndRegisterAdaptersUseCase {

    private val scope get() = CoroutineScope(SdkDispatchers.Bidon)

    override suspend operator fun invoke(
        context: Context,
        adapters: List<Adapter>,
        configResponse: ConfigResponse,
        isTestMode: Boolean
    ) {
        val deferredList = adapters.map { adapter ->
            val demandId = adapter.demandId
            scope.async {
                runCatching {
                    // set test mode param
                    (adapter as? SupportsTestMode)?.isTestMode = isTestMode

                    // initialize if needed
                    val initializable = adapter as? Initializable<AdapterParameters>
                    if (initializable == null) {
                        adapter
                    } else {
                        val timeStart = measureTimeMillis {
                            val adapterParameters =
                                parseAdapterParameters(configResponse, adapter).getOrThrow()
                            adapter.init(context, adapterParameters)
                        }
                        logInfo(TAG, "Adapter ${demandId.demandId} initialized in $timeStart ms.")
                    }
                }.onSuccess {
                    /**
                     * Add adapter to [AdaptersSource] only if it was initialized successfully.
                     */
                    adaptersSource.add(adapter)
                }.onFailure { cause ->
                    logError(TAG, "Adapter not initialized: ${demandId.demandId}", cause)
                }.getOrNull()
            }
        }
        withTimeoutOrNull(configResponse.initializationTimeout) {
            deferredList.forEach { it.await() }
        }
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

private const val TAG = "InitAndRegisterUserCase"
