package org.bidon.sdk.utils.visibilitytracker

import android.view.View
import java.lang.ref.WeakReference

internal class VisibilityTrackerImpl : VisibilityTracker {

    private val holders = mutableListOf<TrackingHolder>()

    override fun startTracking(
        view: View,
        visibilityParams: VisibilityParams,
        visibilityChangeCallback: VisibilityChangeCallback
    ) {
        synchronized(holders) {
            stopTracking(view)
            val viewReference = WeakReference(view)
            val holder = TrackingHolder(viewReference, visibilityParams, visibilityChangeCallback)
            holders.add(holder)
            holder.start()
        }
    }

    override fun stopTracking(view: View) {
        synchronized(holders) {
            val iterator = holders.iterator()
            while (iterator.hasNext()) {
                val holder = iterator.next()
                if (holder.shouldRelease(view)) {
                    holder.release()
                    iterator.remove()
                    break
                }
            }
        }
    }
}