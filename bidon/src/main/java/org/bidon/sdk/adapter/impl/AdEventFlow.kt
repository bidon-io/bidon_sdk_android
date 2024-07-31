package org.bidon.sdk.adapter.impl

import kotlinx.coroutines.flow.SharedFlow
import org.bidon.sdk.adapter.AdEvent

interface AdEventFlow {
    val adEvent: SharedFlow<AdEvent>

    fun emitEvent(event: AdEvent)
}