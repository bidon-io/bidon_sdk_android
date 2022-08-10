package com.appodealstack.bidon.core

import com.appodealstack.bidon.demands.Adapter

internal interface AdaptersSource {
    val adapters: List<Adapter>
    fun add(adapters: List<Adapter>)
}

