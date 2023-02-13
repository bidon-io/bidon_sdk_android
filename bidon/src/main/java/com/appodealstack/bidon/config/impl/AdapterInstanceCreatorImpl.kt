package com.appodealstack.bidon.config.impl

import com.appodealstack.bidon.adapter.Adapter
import com.appodealstack.bidon.logs.logging.impl.logError
import com.appodealstack.bidon.logs.logging.impl.logInfo

/**
 * Created by Aleksei Cherniaev on 10/08/2022.
 */
internal class AdapterInstanceCreatorImpl : com.appodealstack.bidon.config.AdapterInstanceCreator {
    override fun createAvailableAdapters(): List<Adapter> {
        val adaptersClasses = com.appodealstack.bidon.config.DefaultAdapters.values().mapNotNull { adapterItem ->
            obtainServiceClass(adapterItem.classPath)
        }
        logInfo(
            Tag,
            "Available adapters classes: ${adaptersClasses.joinToString { it.simpleName }}"
        )

        return adaptersClasses.mapNotNull {
            getAdapterInstance(clazz = it)
        }
    }

    private fun obtainServiceClass(requiredClass: String): Class<Adapter>? {
        val initialize = false
        val classLoader = this.javaClass.classLoader
        return try {
            @Suppress("UNCHECKED_CAST")
            Class.forName(requiredClass, initialize, classLoader) as Class<Adapter>
        } catch (e: Exception) {
            logError(Tag, "Adapter class not found: $requiredClass", e)
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

private const val Tag = "AdapterInstanceCreator"
