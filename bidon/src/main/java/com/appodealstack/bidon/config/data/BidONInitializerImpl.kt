package com.appodealstack.bidon.config.data

import android.app.Activity
import com.appodealstack.bidon.AdapterList
import com.appodealstack.bidon.config.domain.*
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInfo
import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.demands.Adapter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class BidONInitializerImpl(
    private val adapterRegistry: AdapterRegistry,
    private val configRequestInteractor: ConfigRequestInteractor
) : BidONInitializer {
    override suspend fun init(activity: Activity, appKey: String) {
        val adaptersClasses = AdapterList.values().mapNotNull { adapterItem ->
            obtainServiceClass(adapterItem.classPath)
        }
        logInfo(Tag, "Available adapters classes: $adaptersClasses")
        val adapters = adaptersClasses.mapNotNull {
            getAdapterInstance(clazz = it)
        }
        logInfo(Tag, "Created adapters instances: $adapters")

        val body = ConfigRequestBody(
            adapters = adapters.associate {
                it.demandId.demandId to it.adapterInfo
            }
        )
        logInfo(Tag, "Config request body: ${Json.encodeToString(body)}")

//        configRequestInteractor.request(
//            body = ConfigRequestBody(
//                adapters = adaptersClasses.map {
//                    AdapterInfo()
//                }
//            )
//        )

    }

    private fun obtainServiceClass(requiredClass: String): Class<Adapter>? {
        val initialize = false
        val classLoader = this.javaClass.classLoader
        return try {
            Class.forName(requiredClass, initialize, classLoader) as Class<Adapter>
        } catch (e: Exception) {
            logInternal(Tag, "Adapter class not found: $requiredClass", e)
            null
        }
    }

    private fun getAdapterInstance(clazz: Class<Adapter>): Adapter? {
        return try {
            clazz.newInstance() as Adapter
        } catch (e: Exception) {
            logError(Tag, "Error while creating instance of $clazz", e)
            null
        }
    }
}

private const val Tag = "BidONInitializer"