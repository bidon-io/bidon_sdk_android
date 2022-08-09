package com.appodealstack.bidon.core

import com.appodealstack.bidon.demands.Adapter

internal interface DemandsSource {
    val adapters: List<Adapter>
    fun addDemands(adapter: Adapter)
}

