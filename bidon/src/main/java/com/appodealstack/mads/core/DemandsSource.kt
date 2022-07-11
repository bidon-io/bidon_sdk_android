package com.appodealstack.mads.core

import com.appodealstack.mads.demands.Adapter

internal interface DemandsSource {
    val adapters: List<Adapter<*>>
    fun addDemands(adapter: Adapter<*>)
}

