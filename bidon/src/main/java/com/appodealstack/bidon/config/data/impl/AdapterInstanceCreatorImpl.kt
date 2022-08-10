package com.appodealstack.bidon.config.data.impl

import com.appodealstack.bidon.AdapterList
import com.appodealstack.bidon.config.domain.AdapterInstanceCreator
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInfo
import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.adapters.Adapter

internal class AdapterInstanceCreatorImpl : AdapterInstanceCreator {
    override fun createAvailableAdapters(): List<Adapter> {
        val adaptersClasses = AdapterList.values().mapNotNull { adapterItem ->
            obtainServiceClass(adapterItem.classPath)
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

private const val Tag = "AdapterInstanceCreator"