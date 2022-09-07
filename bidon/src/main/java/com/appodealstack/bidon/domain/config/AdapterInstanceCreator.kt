package com.appodealstack.bidon.domain.config

import com.appodealstack.bidon.domain.adapter.Adapter

internal interface AdapterInstanceCreator {
    fun createAvailableAdapters(): List<Adapter>
}
