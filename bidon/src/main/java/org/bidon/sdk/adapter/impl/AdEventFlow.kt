package org.bidon.sdk.adapter.impl

import kotlinx.coroutines.flow.Flow
import org.bidon.sdk.adapter.AdEvent

interface AdEventFlow {
    val adEvent: Flow<AdEvent>

    fun emitEvent(event: AdEvent)
}