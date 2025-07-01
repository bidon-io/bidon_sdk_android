package org.bidon.sdk.config.impl

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.config.AdapterInstanceCreator
import org.bidon.sdk.config.BidonInitializer
import org.bidon.sdk.config.InitializationCallback
import org.bidon.sdk.config.SdkState
import org.bidon.sdk.config.models.ConfigRequestBody
import org.bidon.sdk.config.usecases.GetConfigRequestUseCase
import org.bidon.sdk.config.usecases.InitAndRegisterAdaptersUseCase
import org.bidon.sdk.databinders.session.SessionTracker
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.segment.SegmentSynchronizer
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.DI
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.keyvaluestorage.KeyValueStorage
import org.bidon.sdk.utils.networking.BidonEndpoints
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class BidonInitializerImpl : BidonInitializer {

    init {
        /**
         * Should be invoked before using all.
         */
        DI.setFactories()
    }

    private val dispatcher by lazy { SdkDispatchers.Bidon }
    private val scope get() = CoroutineScope(dispatcher)

    private var useDefaultAdapters = false
    private var publisherAdapters = mutableMapOf<Class<out Adapter>, Adapter>()
    private var publisherAdapterClasses = mutableSetOf<String>()
    private val initializationCallbacks = CopyOnWriteArraySet<InitializationCallback>()

    private val initAndRegisterAdapters: InitAndRegisterAdaptersUseCase get() = get()
    private val getConfigRequest: GetConfigRequestUseCase get() = get()
    private val adapterInstanceCreator: AdapterInstanceCreator get() = get()
    private val keyValueStorage: KeyValueStorage get() = get()
    private val bidOnEndpoints: BidonEndpoints get() = get()
    private val segmentSynchronizer: SegmentSynchronizer get() = get()

    override val initializationState = MutableStateFlow(SdkState.NotInitialized)
    override val isInitialized: Boolean
        get() = initializationState.value == SdkState.Initialized
    override var isTestMode: Boolean = false
    override val baseUrl: String
        get() = bidOnEndpoints.activeEndpoint

    override fun registerDefaultAdapters() {
        useDefaultAdapters = true
    }

    override fun registerAdapters(vararg adapters: Adapter) {
        adapters.forEach { adapter ->
            publisherAdapters[adapter::class.java] = adapter
        }
    }

    override fun registerAdapter(adaptersClassName: String) {
        publisherAdapterClasses.add(adaptersClassName)
    }

    override fun setInitializationCallback(initializationCallback: InitializationCallback) {
        if (isInitialized) {
            logInfo(TAG, "setInitializationCallback: already initialized")
            initializationCallback.onFinished()
        } else {
            initializationCallbacks.add(initializationCallback)
        }
    }

    override fun setBaseUrl(host: String) {
        bidOnEndpoints.init(host, setOf())
    }

    override fun initialize(context: Context, appKey: String) {
        val timeStart = System.currentTimeMillis()
        if (initializationState.value == SdkState.Initialized) {
            notifyInitialized()
            return
        }
        val isNotInitialized = initializationState.compareAndSet(
            expect = SdkState.NotInitialized,
            update = SdkState.Initializing
        )
        if (isNotInitialized) {
            /**
             * [DI.init] must be invoked before using all.
             * Check if SDK is initialized with [isInitialized].
             */
            DI.init(context)
            scope.launch {
                obtainSegmentUid()
                runCatching {
                    init(context, appKey, timeStart)
                }.onFailure {
                    logError(TAG, "Error while initialization", it)
                    initializationState.value = SdkState.InitializationFailed
                }.onSuccess {
                    logInfo(TAG, "Initialized in ${System.currentTimeMillis() - timeStart} ms.")
                    initializationState.value = SdkState.Initialized
                }
                notifyInitialized()
            }
        }
    }

    private suspend fun obtainSegmentUid() {
        withContext(SdkDispatchers.IO) {
            keyValueStorage.segmentUid?.let {
                segmentSynchronizer.setSegmentUid(segmentUid = it)
            }
        }
    }

    private suspend fun init(context: Context, appKey: String, timeStart: Long): Result<Unit> {
        startSession()
        withContext(SdkDispatchers.IO) {
            keyValueStorage.appKey = appKey
        }
        val defaultAdapters = adapterInstanceCreator.createAvailableAdapters(
            useDefaultAdapters = useDefaultAdapters,
            adapterClasses = publisherAdapterClasses
        )

        logInfo(TAG, "Created adapters instances: $defaultAdapters")
        val body = ConfigRequestBody(
            adapters = (defaultAdapters + publisherAdapters.values).associate {
                it.demandId.demandId to it.adapterInfo
            }
        )
        return getConfigRequest.request(body)
            .map { configResponse ->
                logInfo(
                    TAG,
                    "Config data received in ${System.currentTimeMillis() - timeStart} ms.: $configResponse"
                )
                logInfo(TAG, "Starting adapters initialization")
                initAndRegisterAdapters(
                    context = context,
                    adapters = (defaultAdapters + publisherAdapters.values).distinctBy { it::class },
                    configResponse = configResponse,
                    isTestMode = isTestMode
                )
            }.onFailure {
                logError(TAG, "Error while Config-request", it)
            }
    }

    private fun startSession() {
        /**
         * Just retrieve instance to start session time
         */
        val sessionTracker = get<SessionTracker>()
        logInfo(TAG, "Session started with sessionId=${sessionTracker.sessionId}")
    }

    private fun notifyInitialized() {
        initializationCallbacks.forEach { callback ->
            logInfo(TAG, "notifyInitialized: notified callback: $callback")
            callback.onFinished()
        }
        publisherAdapters.clear()
        initializationCallbacks.clear()
    }
}

private const val TAG = "BidonInitializer"
