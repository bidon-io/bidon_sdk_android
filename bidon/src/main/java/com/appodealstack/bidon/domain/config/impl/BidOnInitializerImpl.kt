package com.appodealstack.bidon.domain.config.impl

import android.app.Activity
import com.appodealstack.bidon.data.binderdatasources.session.SessionTracker
import com.appodealstack.bidon.data.keyvaluestorage.KeyValueStorage
import com.appodealstack.bidon.data.models.config.ConfigRequestBody
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.domain.adapter.Adapter
import com.appodealstack.bidon.domain.common.SdkState
import com.appodealstack.bidon.domain.config.AdapterInstanceCreator
import com.appodealstack.bidon.domain.config.BidOnInitializer
import com.appodealstack.bidon.domain.config.usecases.GetConfigRequestUseCase
import com.appodealstack.bidon.domain.config.usecases.InitAndRegisterAdaptersUseCase
import com.appodealstack.bidon.domain.stats.impl.logError
import com.appodealstack.bidon.domain.stats.impl.logInfo
import com.appodealstack.bidon.view.helper.SdkDispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class BidOnInitializerImpl(
    private val initAndRegisterAdapters: InitAndRegisterAdaptersUseCase,
    private val getConfigRequest: GetConfigRequestUseCase,
    private val adapterInstanceCreator: AdapterInstanceCreator,
    private val keyValueStorage: KeyValueStorage,
) : BidOnInitializer {
    private val sdkState = MutableStateFlow(SdkState.NotInitialized)
    private var useDefaultAdapters = false
    private var publisherAdapters = mutableMapOf<Class<out Adapter>, Adapter>()

    override val isInitialized: Boolean
        get() = sdkState.value == SdkState.Initialized

    override fun withDefaultAdapters() {
        useDefaultAdapters = true
    }

    override fun withAdapters(vararg adapters: Adapter) {
        adapters.forEach { adapter ->
            publisherAdapters[adapter::class.java] = adapter
        }
    }

    override suspend fun init(activity: Activity, appKey: String): Result<Unit> {
        startSession()
        if (sdkState.compareAndSet(expect = SdkState.NotInitialized, update = SdkState.Initializing)) {
            withContext(SdkDispatchers.IO) {
                keyValueStorage.appKey = appKey
            }
            val adapters = if (useDefaultAdapters) {
                adapterInstanceCreator.createAvailableAdapters()
            } else {
                emptyList()
            }
            logInfo(Tag, "Created adapters instances: $adapters")

            val body = ConfigRequestBody(
                adapters = (adapters + publisherAdapters.values).associate {
                    it.demandId.demandId to it.adapterInfo
                }
            )
            return getConfigRequest.request(body)
                .map { configResponse ->
                    logInfo(Tag, "Config data: $configResponse")
                    initAndRegisterAdapters(
                        activity = activity,
                        notInitializedAdapters = adapters,
                        publisherAdapters = publisherAdapters.values.toList(),
                        configResponse = configResponse,
                    )
                }.onFailure {
                    logError(Tag, "Error while Config-request", it)
                    it.printStackTrace()
                    sdkState.value = SdkState.InitializationFailed
                }.onSuccess {
                    sdkState.value = SdkState.Initialized
                }
        } else {
            sdkState.first { it == SdkState.Initialized || it == SdkState.InitializationFailed }
            return Result.success(Unit)
        }
    }

    private fun startSession() {
        /**
         * Just retrieve instance to start session time
         */
        val sessionTracker = get<SessionTracker>()
        logInfo(Tag, "Session started with sessionId=${sessionTracker.sessionId}")
    }
}

private const val Tag = "BidONInitializer"
