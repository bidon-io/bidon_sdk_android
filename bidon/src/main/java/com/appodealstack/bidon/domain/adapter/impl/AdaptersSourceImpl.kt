package com.appodealstack.bidon.domain.adapter.impl

import com.appodealstack.bidon.domain.adapter.Adapter
import com.appodealstack.bidon.domain.adapter.AdaptersSource

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class AdaptersSourceImpl : AdaptersSource {
    override val adapters = mutableSetOf<Adapter>()

    override fun add(adapters: Collection<Adapter>) {
        this.adapters.addAll(adapters)
    }
}