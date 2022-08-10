package com.appodealstack.bidon.core.impl

import com.appodealstack.bidon.core.AdaptersSource
import com.appodealstack.bidon.adapters.Adapter

internal class AdaptersSourceImpl : AdaptersSource {
    override val adapters = mutableListOf<Adapter>()

    override fun add(adapters: List<Adapter>) {
        this.adapters.addAll(adapters)
    }
}