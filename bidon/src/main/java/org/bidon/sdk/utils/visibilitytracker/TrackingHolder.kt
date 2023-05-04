package org.bidon.sdk.utils.visibilitytracker

import android.view.View
import android.view.ViewTreeObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.SdkDispatchers
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

internal class TrackingHolder(
    private val viewReference: WeakReference<View>,
    private val visibilityParams: VisibilityParams,
    private val visibilityChangeCallback: VisibilityChangeCallback,
    private val scope: CoroutineScope = CoroutineScope(SdkDispatchers.Main),
) {
    private val shownTracked = AtomicBoolean(false)
    private val preDrawListener = ViewTreeObserver.OnPreDrawListener { scheduleObserver() }
    private val requiredOnScreenTime = visibilityParams.timeThresholdMs
    private var shownObserverJob: Job? = null

    fun start() {
        val view = viewReference.get()
        if (view == null) {
            release()
        } else {
            logInfo(Tag, "Start tracking - $view")
            view.viewTreeObserver.addOnPreDrawListener(preDrawListener)
        }
    }

    private fun scheduleObserver(): Boolean {
        if (shownObserverJob == null) {
            shownObserverJob = scope.launch { proceedShown() }
        }
        return true
    }

    private suspend fun proceedShown() {
        val view = viewReference.get()
        if (view == null || shownTracked.get()) {
            release()
        } else if (view.isOnTop(visibilityParams)) {
            delay(requiredOnScreenTime)
            if (view.isOnTop(visibilityParams)) {
                if (shownTracked.compareAndSet(false, true)) {
                    visibilityChangeCallback.onViewShown()
                }
                release()
            } else {
                shownObserverJob?.cancel()
                shownObserverJob = null
                scheduleObserver()
            }
        } else {
            shownObserverJob?.cancel()
            shownObserverJob = null
            delay(DefCheckDelay)
            scheduleObserver()
        }
    }

    fun shouldRelease(view: View): Boolean {
        return viewReference.get() == null || viewReference.get() === view
    }

    fun release() {
        val view = viewReference.get()
        if (view != null) {
            logInfo(Tag, "Stop tracking - $view")
            view.viewTreeObserver.removeOnPreDrawListener(preDrawListener)
        }
        viewReference.clear()
        shownObserverJob?.cancel()
        shownObserverJob = null
    }
}

private const val DefCheckDelay = 100L
private const val Tag = "VisibilityTracker"
