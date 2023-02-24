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
        adapters: List<Adapter>,
        configResponse: ConfigResponse
    ) {
        val timeout = configResponse.initializationTimeout
        val readyAdapters = adapters.mapNotNull { adapter ->
            val initializable = adapter as? Initializable<AdapterParameters>
                ?: return@mapNotNull run {
                    // Adapter is not Initializable. It's ready to use
                    adapter
                }

            val adapterParameters = configResponse.adapters
                .firstNotNullOfOrNull { (adapterName, json) ->
                    if (adapter.demandId.demandId == adapterName) {
                        try {
                            initializable.parseConfigParam(json.toString())
                        } catch (e: Exception) {
                            logError(Tag, "Error while parsing AdapterParameters for ${adapter.demandId.demandId}: $json", e)
                            null
                        }
                    } else {
                        null
                    }
                }

            if (adapterParameters == null) {
                logError(Tag, "Config parameters is null. Adapter not initialized: $initializable", null)
                null
            } else {
                withTimeoutOrNull(timeout) {
                    runCatching {
                        initializable.init(
                            activity = activity,
                            configParams = adapterParameters
                        )
                        adapter
                    }.getOrNull()
                } ?: run {
                    logError(Tag, "Adapter's initializing timed out. Adapter not initialized: $initializable", null)
                    null
                }
            }
        }
        logInfo(Tag, "Registered adapters: ${readyAdapters.joinToString { it::class.java.simpleName }}")
        adaptersSource.add(readyAdapters)
    }
}

private const val Tag = "InitAndRegisterUserCase"
