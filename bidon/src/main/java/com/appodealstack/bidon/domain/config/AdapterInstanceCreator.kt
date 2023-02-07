package com.appodealstack.bidon.domain.config

import com.appodealstack.bidon.domain.adapter.Adapter
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface AdapterInstanceCreator {
    fun createAvailableAdapters(): List<Adapter>
}
