package com.appodealstack.bidon.config

import com.appodealstack.bidon.adapter.Adapter

/**
 * Created by Aleksei Cherniaev on 10/08/2022.
 */
internal interface AdapterInstanceCreator {
    fun createAvailableAdapters(useDefaultAdapters: Boolean, adapterClasses: MutableSet<String>): List<Adapter>
}
