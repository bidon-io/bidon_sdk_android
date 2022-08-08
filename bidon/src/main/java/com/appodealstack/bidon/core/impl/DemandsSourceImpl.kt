package com.appodealstack.bidon.core.impl

import com.appodealstack.bidon.core.DemandsSource
import com.appodealstack.bidon.demands.Adapter

internal class DemandsSourceImpl : DemandsSource {
    override val adapters = mutableListOf<Adapter<*>>()

    override fun addDemands(adapter: Adapter<*>) {
        this.adapters.add(adapter)
    }
}