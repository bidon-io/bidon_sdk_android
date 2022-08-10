package com.appodealstack.bidon.config.domain

import com.appodealstack.bidon.adapters.Adapter

internal interface AdapterInstanceCreator {
    fun createAvailableAdapters(): List<Adapter>
}