package com.appodealstack.bidon.domain.adapter.impl

import com.appodealstack.bidon.domain.adapter.Adapter
import com.appodealstack.bidon.domain.adapter.AdaptersSource

internal class AdaptersSourceImpl : AdaptersSource {
    override val adapters = mutableListOf<Adapter>()

    override fun add(adapters: List<Adapter>) {
        this.adapters.addAll(adapters)
    }
}
