package org.bidon.sdk.utils.visibilitytracker

import android.view.View
import android.view.ViewTreeObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.bidon.sdk.ads.banner.helper.ActivityLifecycleState
import org.bidon.sdk.ads.banner.helper.PauseResumeObserver
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get
import java.util.concurrent.atomic.AtomicBoolean

internal class VisibilityTracker(
    private val visibilityParams: VisibilityParams = VisibilityParams(),
    private val scope: CoroutineScope = CoroutineScope(SdkDispatchers.Main),
    private val pauseResumeObserver: PauseResumeObserver = get()
) {
    private val isStarted = AtomicBoolean(false)
    private val showTracked = AtomicBoolean(false)
    private val preDrawListener = ViewTreeObserver.OnPreDrawListener {
        checkVisible()
        true
    }
    private val requiredOnScreenTime get() = visibilityParams.timeThresholdMs
    private var shownObserverJob: Job? = null
    private var view: View? = null
    private var onViewShown: (() -> Unit)? = null

    fun start(
        view: View,
        onViewShown: () -> Unit,
    ) {
        if (isStarted.compareAndSet(/* expectedValue = */ false, /* newValue = */ true)) {
            this.onViewShown = onViewShown
            this.view = view
            logInfo(Tag, "Start tracking - $view")
            view.viewTreeObserver.addOnPreDrawListener(preDrawListener)
            checkVisible()
        }
    }

    fun stop() {
        logInfo(Tag, "Stop tracking - $view")
        view?.viewTreeObserver?.removeOnPreDrawListener(preDrawListener)
        shownObserverJob?.cancel()
        shownObserverJob = null
        view = null
        onViewShown = null
    }

    private fun checkVisible() {
        if (showTracked.get()) return
        if (shownObserverJob?.isActive == true) return
        shownObserverJob?.cancel()
        shownObserverJob = scope.launch {
            pauseResumeObserver.lifecycleFlow.first { state ->
                (state == ActivityLifecycleState.Resumed).also {
                    if (!it) {
                        logInfo(Tag, "Paused. Application in background.")
                    }
                }
            }
            if (view.isOnTop(visibilityParams)) {
                delay(requiredOnScreenTime)
                if (view.isOnTop(visibilityParams)) {
                    if (showTracked.compareAndSet(false, true)) {
                        logInfo(Tag, "Tracked - $view")
                        onViewShown?.invoke()
                    }
                    stop()
                } else {
                    shownObserverJob?.cancel()
                    checkVisible()
                }
            } else {
                delay(DefCheckDelay)
                shownObserverJob?.cancel()
                checkVisible()
            }
        }
    }
}

private const val DefCheckDelay = 100L
private const val Tag = "VisibilityTracker"
