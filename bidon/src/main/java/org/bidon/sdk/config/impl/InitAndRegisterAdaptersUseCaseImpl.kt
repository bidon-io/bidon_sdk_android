package org.bidon.sdk.config.impl

import android.app.Activity
import kotlinx.coroutines.withTimeoutOrNull
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.config.models.ConfigResponse
import org.bidon.sdk.config.usecases.InitAndRegisterAdaptersUseCase
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
@Suppress("UNCHECKED_CAST")
internal class InitAndRegisterAdaptersUseCaseImpl(
    private val adaptersSource: AdaptersSource
) : InitAndRegisterAdaptersUseCase {

    override suspend operator fun invoke(
        activity: Activity,
        notInitializedAdapters: List<Adapter>,
        publisherAdapters: List<Adapter>,
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
                            adapter.parseConfigParam(json.toString())
                        } catch (e: Exception) {
                            logError(Tag, "Error while parsing AdapterParameters for ${adapter.demandId.demandId}: $json", e)
                            null
                        }
                    } else {
                        null
                    }
                }

            if (adapterParameters == null) {
                logError(Tag, "Config parameters is null. Adapter not initialized: $adapter", null)
                null
            } else {
                withTimeoutOrNull(timeout) {
                    initializable.init(
                        activity = activity,
                        configParams = adapterParameters
                    )
                    adapter
                } ?: run {
                    logError(Tag, "Adapter's initializing timed out. Adapter not initialized: $adapter", null)
                    null
                }
            }
        }
        val adapters = readyAdapters + publisherAdapters
        logInfo(Tag, "Registered adapters: ${adapters.joinToString { it::class.java.simpleName }}")
        adaptersSource.add(adapters)
    }
}

private const val Tag = "InitAndRegisterUserCase"
