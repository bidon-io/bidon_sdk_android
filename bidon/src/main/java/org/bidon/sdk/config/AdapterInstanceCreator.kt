package org.bidon.sdk.config

import org.bidon.sdk.adapter.Adapter

/**
 * Created by Aleksei Cherniaev on 10/08/2022.
 */
internal interface AdapterInstanceCreator {
    fun createAvailableAdapters(useDefaultAdapters: Boolean, adapterClasses: MutableSet<String>): List<Adapter>
}
