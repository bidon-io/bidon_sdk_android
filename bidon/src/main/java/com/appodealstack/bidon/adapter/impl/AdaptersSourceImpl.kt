package com.appodealstack.bidon.adapter.impl

import com.appodealstack.bidon.adapter.Adapter
import com.appodealstack.bidon.adapter.AdaptersSource

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class AdaptersSourceImpl : AdaptersSource {
    override val adapters = mutableSetOf<Adapter>()

    override fun add(adapters: Collection<Adapter>) {
        this.adapters.addAll(adapters)
    }
}
