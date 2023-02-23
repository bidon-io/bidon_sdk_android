package org.bidon.sdk.config.impl

import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.config.DefaultAdapters
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo

/**
 * Created by Aleksei Cherniaev on 10/08/2022.
 */
internal class AdapterInstanceCreatorImpl : org.bidon.sdk.config.AdapterInstanceCreator {
    override fun createAvailableAdapters(useDefaultAdapters: Boolean, adapterClasses: MutableSet<String>): List<Adapter> {
        val classes = adapterClasses + DefaultAdapters.values().map { it.classPath }.takeIf { useDefaultAdapters }.orEmpty()
        val adaptersClasses = classes.mapNotNull { clazz ->
            obtainServiceClass(clazz)
        }
        logInfo(Tag, "Available adapters classes: ${adaptersClasses.joinToString { it.simpleName }}")
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
