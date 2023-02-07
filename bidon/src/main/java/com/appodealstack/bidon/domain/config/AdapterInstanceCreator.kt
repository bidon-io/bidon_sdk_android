package com.appodealstack.bidon.domain.config

import com.appodealstack.bidon.domain.adapter.Adapter

/**
 * Created by Aleksei Cherniaev on 10/08/2022.
 */
internal interface AdapterInstanceCreator {
    fun createAvailableAdapters(): List<Adapter>
}
