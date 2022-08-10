package com.appodealstack.bidon.config.domain.impl

import android.app.Activity
import com.appodealstack.bidon.config.data.models.ConfigRequestBody
import com.appodealstack.bidon.config.domain.*
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInfo

internal class BidONInitializerImpl(
    private val initAndRegisterAdapters: InitAndRegisterAdaptersUseCase,
    private val configRequestInteractor: ConfigRequestInteractor,
    private val adapterInstanceCreator: AdapterInstanceCreator
) : BidONInitializer {
    override suspend fun init(activity: Activity, appKey: String): Result<Unit> {
        val adapters = adapterInstanceCreator.createAvailableAdapters()
        logInfo(Tag, "Created adapters instances: $adapters")

        val body = ConfigRequestBody(
            adapters = adapters.associate {
                it.demandId.demandId to it.adapterInfo
            }
        )
        logInfo(Tag, "Config request body: ${body.getJson()}")

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
    }
}

private const val Tag = "BidONInitializer"