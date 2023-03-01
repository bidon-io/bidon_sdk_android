package org.bidon.sdk.config.impl

import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.config.BidonInitializer
import org.bidon.sdk.config.SdkState
import org.bidon.sdk.config.models.ConfigRequestBody
import org.bidon.sdk.config.usecases.GetConfigRequestUseCase
import org.bidon.sdk.config.usecases.InitAndRegisterAdaptersUseCase
import org.bidon.sdk.databinders.session.SessionTracker
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.DI
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.keyvaluestorage.KeyValueStorage
import org.bidon.sdk.utils.networking.BidonEndpoints

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class BidonInitializerImpl : BidonInitializer {
    private val dispatcher by lazy { SdkDispatchers.Single }
    private val scope get() = CoroutineScope(dispatcher)

    private var useDefaultAdapters = false
    private var publisherAdapters = mutableMapOf<Class<out Adapter>, Adapter>()
    private var publisherAdapterClasses = mutableSetOf<String>()
    private var initializationCallback: org.bidon.sdk.config.InitializationCallback? = null
    private val initializationState = MutableStateFlow(SdkState.NotInitialized)

    private val initAndRegisterAdapters: InitAndRegisterAdaptersUseCase get() = get()
    private val getConfigRequest: GetConfigRequestUseCase get() = get()
    private val adapterInstanceCreator: org.bidon.sdk.config.AdapterInstanceCreator get() = get()
    private val keyValueStorage: KeyValueStorage get() = get()
    private val bidOnEndpoints: BidonEndpoints get() = get()

    init {
        DI.setFactories()
    }

    override val isInitialized: Boolean
        get() = initializationState.value == SdkState.Initialized

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

    override fun setInitializationCallback(initializationCallback: org.bidon.sdk.config.InitializationCallback) {
        this.initializationCallback = initializationCallback
    }

    override fun setBaseUrl(host: String) {
        bidOnEndpoints.init(host, setOf())
    }

    override fun initialize(activity: Activity, appKey: String) {
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
            DI.init(context = activity.applicationContext)
            scope.launch {
                runCatching {
                    init(activity, appKey)
                }.onFailure {
                    logError(Tag, "Error while initialization", it)
                    initializationState.value = SdkState.InitializationFailed
                }.onSuccess {
                    initializationState.value = SdkState.Initialized
                }
                notifyInitialized()
            }
        }
    }

    private suspend fun init(activity: Activity, appKey: String): Result<Unit> {
        startSession()
        withContext(SdkDispatchers.IO) {
            keyValueStorage.appKey = appKey
        }
        val defaultAdapters = adapterInstanceCreator.createAvailableAdapters(
            useDefaultAdapters = useDefaultAdapters,
            adapterClasses = publisherAdapterClasses
        )

        logInfo(Tag, "Created adapters instances: $defaultAdapters")
        val body = ConfigRequestBody(
            adapters = (defaultAdapters + publisherAdapters.values).associate {
                it.demandId.demandId to it.adapterInfo
            }
        )
        return getConfigRequest.request(body)
            .map { configResponse ->
                logInfo(Tag, "Config data: $configResponse")
                initAndRegisterAdapters(
                    activity = activity,
                    adapters = defaultAdapters + publisherAdapters.values,
                    configResponse = configResponse,
                )
            }.onFailure {
                logError(Tag, "Error while Config-request", it)
            }
    }

    private fun startSession() {
        /**
         * Just retrieve instance to start session time
         */
        val sessionTracker = get<SessionTracker>()
        logInfo(Tag, "Session started with sessionId=${sessionTracker.sessionId}")
    }

    private fun notifyInitialized() {
        initializationCallback?.onFinished()
        publisherAdapters.clear()
        initializationCallback = null
    }
}

private const val Tag = "BidonInitializer"
