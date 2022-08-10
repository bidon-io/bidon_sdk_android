package com.appodealstack.bidon.config.domain

import com.appodealstack.bidon.demands.Adapter

internal interface AdapterInstanceCreator {
    fun createAvailableAdapters(): List<Adapter>
}