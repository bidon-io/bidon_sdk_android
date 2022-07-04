package com.appodealstack.mads.demands

internal interface DemandsSource {
    val demands: List<Demand>
    fun addDemands(demand: Demand)
}

internal class DemandsSourceImpl : DemandsSource {
    override val demands = mutableListOf<Demand>()

    override fun addDemands(demand: Demand) {
        this.demands.add(demand)
    }
}