package com.appodealstack.bidon.domain.adapter

internal interface AdaptersSource {
    val adapters: List<Adapter>
    fun add(adapters: List<Adapter>)
}
