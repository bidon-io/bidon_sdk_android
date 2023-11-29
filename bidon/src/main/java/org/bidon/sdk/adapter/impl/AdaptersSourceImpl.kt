package org.bidon.sdk.adapter.impl

import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdaptersSource

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal class AdaptersSourceImpl : AdaptersSource {
    override val adapters = mutableSetOf<Adapter>()

    override fun add(adapter: Adapter) {
        this.adapters.add(adapter)
    }
}
