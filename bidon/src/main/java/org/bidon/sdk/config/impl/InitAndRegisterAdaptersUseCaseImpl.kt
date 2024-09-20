package org.bidon.sdk.config.impl

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
import org.json.JSONObject
import kotlin.system.measureTimeMillis

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class InitAndRegisterAdaptersUseCaseImpl(
    private val adaptersSource: AdaptersSource
) : InitAndRegisterAdaptersUseCase {

    private val scope get() = CoroutineScope(SdkDispatchers.Bidon + SupervisorJob())
    private val canContinueFlow = MutableStateFlow(false)

    override suspend operator fun invoke(
        context: Context,
        adapters: List<Adapter>,
        configResponse: ConfigResponse,
        isTestMode: Boolean
    ) {
        // set test mode param
        adapters.forEach {
            (it as? SupportsTestMode)?.isTestMode = isTestMode
        }

        // start initialization
        canContinueFlow
            .onSubscription {
                scope.launch {
                    delay(configResponse.initializationTimeout)
                    /**
                     * Continue initialization flow after timeout.
                     * Not initialized adapters will be added to [AdaptersSource] after timeout.
                     */
                    canContinueFlow.update { isInitializedOnTime ->
                        if (!isInitializedOnTime) {
                            val initializedAdapters = adaptersSource.adapters.joinToString { it.demandId.demandId }
                            logError(TAG, "Timeout reached. Available adapters: $initializedAdapters", null)
                        }
                        true
                    }
                }
                scope.launch {
                    /**
                     * Initialize adapters
                     */
                    initializeAdapters(adapters, configResponse, context)
                }
            }.first { canContinue ->
                /**
                 * Wait for ability to continue initialization flow.
                 */
                canContinue
            }
        logInfo(TAG, "Registered adapters: ${adaptersSource.adapters.joinToString { it::class.java.simpleName }}")
    }

    private suspend fun initializeAdapters(
        adapters: List<Adapter>,
        configResponse: ConfigResponse,
        context: Context
    ) {
        runCatching {
            val adapterList = adapters.toMutableSet()
            logInfo(TAG, "Adapters: ${adapterList.joinToString { it.demandId.demandId }}")
            val groupedAdapters = configResponse.adapters.toList()
                .groupBy { (_, initJson) -> initJson.optInt("order", 0) }
                .toSortedMap()
                .onEach { (order, adaptersInfo) ->
                    logInfo(TAG, "Initialization order #$order: ${adaptersInfo.joinToString { it.first }}")
                }
            groupedAdapters.forEach { (order, adaptersInfo) ->
                logInfo(TAG, "Start initialization #$order: ${adaptersInfo.joinToString { it.first }}")
                initializeAdapterGroup(
                    context = context,
                    adaptersInfo = adaptersInfo,
                    adapters = adapterList,
                    configResponse = configResponse,
                    onAdapterInitializationStarted = {
                        adapterList.removeAll(it)
                    }
                )
            }
            canContinueFlow.update { isTimedOut ->
                if (isTimedOut) {
                    logError(TAG, "Initialization finished after timeout ${configResponse.initializationTimeout} ms reached", null)
                }
                true
            }
        }
    }

    private suspend fun initializeAdapterGroup(
        context: Context,
        adaptersInfo: List<Pair<String, JSONObject>>,
        adapters: Set<Adapter>,
        configResponse: ConfigResponse,
        onAdapterInitializationStarted: (adapters: Set<Adapter>) -> Unit
    ) {
        val nextAdaptersGroup = adaptersInfo
            .mapNotNull { (demandId, _) ->
                adapters.find { it.demandId.demandId == demandId }
            }.also {
                onAdapterInitializationStarted(it.toSet())
            }
        val deferredList = nextAdaptersGroup.map { adapter ->
            scope.async {
                runCatching {
                    initializeSingleAdapter(adapter, context, configResponse)
                }.onSuccess {
                    /**
                     * Add adapter to [AdaptersSource] only if it was initialized successfully.
                     */
                    adaptersSource.add(adapter)
                }.onFailure { cause ->
                    logError(TAG, "Adapter not initialized: ${adapter.demandId.demandId}: ${cause.message}", cause)
                }
            }
        }
        withTimeoutOrNull(configResponse.initializationTimeout) {
            deferredList.awaitAll()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun initializeSingleAdapter(
        adapter: Adapter,
        context: Context,
        configResponse: ConfigResponse
    ) {
        val initializable = adapter as? Initializable<AdapterParameters>
        if (initializable != null) {
            val measuredTime = measureTimeMillis {
                val adapterParameters =
                    parseAdapterParameters(configResponse, initializable).getOrThrow()
                adapter.init(context, adapterParameters)
            }
            logInfo(TAG, "Adapter ${adapter.demandId.demandId} initialized in $measuredTime ms.")
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
