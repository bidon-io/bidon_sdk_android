package com.appodealstack.bidon.core.impl

import com.appodealstack.bidon.adapters.Adapter
import com.appodealstack.bidon.core.AdaptersSource

internal class AdaptersSourceImpl : AdaptersSource {
    override val adapters = mutableListOf<Adapter>()

    override fun add(adapters: List<Adapter>) {
        this.adapters.addAll(adapters)
    }
}
