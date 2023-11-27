package org.bidon.sdk.adapter.impl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdaptersSource

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal class AdaptersSourceImpl : AdaptersSource {
    private val adaptersFlow = MutableStateFlow<Set<Adapter>>(emptySet())

    override val adapters
        get() = adaptersFlow.value

    override fun add(adapter: Adapter) {
        adaptersFlow.update {
            it + adapter
        }
    }
}
