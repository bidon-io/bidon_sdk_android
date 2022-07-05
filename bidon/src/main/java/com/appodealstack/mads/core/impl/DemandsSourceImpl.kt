package com.appodealstack.mads.core.impl

import com.appodealstack.mads.core.DemandsSource
import com.appodealstack.mads.demands.Demand

internal class DemandsSourceImpl : DemandsSource {
    override val demands = mutableListOf<Demand>()

    override fun addDemands(demand: Demand) {
        this.demands.add(demand)
    }
}