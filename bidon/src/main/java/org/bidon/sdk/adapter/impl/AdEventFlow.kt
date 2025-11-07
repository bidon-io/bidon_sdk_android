package org.bidon.sdk.adapter.impl

import kotlinx.coroutines.flow.SharedFlow
import org.bidon.sdk.adapter.AdEvent

public interface AdEventFlow {
    public val adEvent: SharedFlow<AdEvent>

    public fun emitEvent(event: AdEvent)
}