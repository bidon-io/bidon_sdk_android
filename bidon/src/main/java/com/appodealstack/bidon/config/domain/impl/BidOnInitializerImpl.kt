package com.appodealstack.bidon.config.domain.impl

import android.app.Activity
import com.appodealstack.bidon.SdkState
import com.appodealstack.bidon.config.data.models.ConfigRequestBody
import com.appodealstack.bidon.config.domain.AdapterInstanceCreator
import com.appodealstack.bidon.config.domain.BidOnInitializer
import com.appodealstack.bidon.config.domain.ConfigRequestInteractor
import com.appodealstack.bidon.config.domain.InitAndRegisterAdaptersUseCase
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate

internal class BidOnInitializerImpl(
    private val initAndRegisterAdapters: InitAndRegisterAdaptersUseCase,
    private val configRequestInteractor: ConfigRequestInteractor,
    private val adapterInstanceCreator: AdapterInstanceCreator
) : BidOnInitializer {
    private val sdkState = MutableStateFlow(SdkState.NotInitialized)

    override val isInitialized: Boolean
        get() = sdkState.value == SdkState.Initialized

    override suspend fun init(activity: Activity, appKey: String): Result<Unit> {
        if (sdkState.compareAndSet(expect = SdkState.NotInitialized, update = SdkState.Initializing)) {
            val adapters = adapterInstanceCreator.createAvailableAdapters()
            logInfo(Tag, "Created adapters instances: $adapters")

            val body = ConfigRequestBody(
                adapters = adapters.associate {
                    it.demandId.demandId to it.adapterInfo
                }
            )
            return configRequestInteractor.request(body)
                .map { configResponse ->
                    logInfo(Tag, "Config data: $configResponse")
                    initAndRegisterAdapters(
                        activity = activity,
                        notInitializedAdapters = adapters,
                        configResponse = configResponse
                    )
                }.onFailure {
                    logError(Tag, "Error while Config-request", it)
                    it.printStackTrace()
                }
        } else {
            sdkState.first { it == SdkState.Initialized }
            return Result.success(Unit)
        }
    }
}

private const val Tag = "BidONInitializer"