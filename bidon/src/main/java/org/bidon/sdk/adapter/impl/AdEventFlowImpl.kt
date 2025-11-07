package org.bidon.sdk.adapter.impl

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.utils.ext.TAG

/**
 * Created by Aleksei Cherniaev on 25/07/2023.
 */
public class AdEventFlowImpl : AdEventFlow {
    /**
     * [PastEvent] for details
     */
    private val pastEvents = MutableStateFlow(
        PastEvent(
            loaded = false,
            loadFailed = false,
            shown = false,
            showFailed = false,
            clicked = false,
            expired = false,
            impression = false,
            rewarded = false,
            closed = false
        )
    )

    override val adEvent: MutableSharedFlow<AdEvent> by lazy {
        MutableSharedFlow<AdEvent>(
            extraBufferCapacity = Int.MAX_VALUE,
            replay = 0,
            onBufferOverflow = BufferOverflow.SUSPEND
        )
    }

    override fun emitEvent(event: AdEvent) {
        if (didEventPass(event)) {
            logError(TAG, "Event ${event::class.simpleName} already passed", null)
            return
        }
        adEvent.tryEmit(event)
    }

    private fun didEventPass(event: AdEvent): Boolean {
        return when (event) {
            is AdEvent.Fill -> pastEvents.getAndUpdate { it.copy(loaded = true) }.loaded
            is AdEvent.LoadFailed -> pastEvents.getAndUpdate { it.copy(loadFailed = true) }.loadFailed
            is AdEvent.Shown -> pastEvents.getAndUpdate { it.copy(shown = true) }.shown
            is AdEvent.ShowFailed -> pastEvents.getAndUpdate { it.copy(showFailed = true) }.showFailed
            is AdEvent.PaidRevenue -> pastEvents.getAndUpdate { it.copy(impression = true) }.impression
            is AdEvent.Clicked -> pastEvents.getAndUpdate { it.copy(clicked = true) }.clicked
            is AdEvent.OnReward -> pastEvents.getAndUpdate { it.copy(rewarded = true) }.rewarded
            is AdEvent.Closed -> pastEvents.getAndUpdate { it.copy(closed = true) }.closed
            is AdEvent.Expired -> pastEvents.getAndUpdate { it.copy(expired = true) }.expired
        }
    }

    /**
     * Some networks can send multiple events for the same action. OnClick for example.
     */
    private data class PastEvent(
        val loaded: Boolean,
        val loadFailed: Boolean,
        val shown: Boolean,
        val showFailed: Boolean,
        val clicked: Boolean,
        val expired: Boolean,
        val impression: Boolean,
        val rewarded: Boolean,
        val closed: Boolean,
    )
}
