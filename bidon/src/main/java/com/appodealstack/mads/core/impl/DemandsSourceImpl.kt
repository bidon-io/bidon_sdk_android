package com.appodealstack.mads.core.impl

import com.appodealstack.mads.core.DemandsSource
import com.appodealstack.mads.demands.Adapter

internal class DemandsSourceImpl : DemandsSource {
    override val adapters = mutableListOf<Adapter>()

    override fun addDemands(adapter: Adapter) {
        this.adapters.add(adapter)
    }
}