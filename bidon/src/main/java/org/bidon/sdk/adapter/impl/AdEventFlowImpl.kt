package org.bidon.sdk.adapter.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bidon.sdk.adapter.AdEvent

/**
 * Created by Aleksei Cherniaev on 25/07/2023.
 */
class AdEventFlowImpl : AdEventFlow {
    override val adEvent by lazy {
        MutableSharedFlow<AdEvent>(extraBufferCapacity = Int.MAX_VALUE, replay = 0)
    }
}

interface AdEventFlow {
    val adEvent: Flow<AdEvent>

    fun emitEvent(event: AdEvent) {
        (adEvent as MutableSharedFlow).tryEmit(event)
    }
}
