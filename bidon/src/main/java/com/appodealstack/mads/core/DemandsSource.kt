package com.appodealstack.mads.core

import com.appodealstack.mads.demands.Demand

internal interface DemandsSource {
    val demands: List<Demand>
    fun addDemands(demand: Demand)
}

