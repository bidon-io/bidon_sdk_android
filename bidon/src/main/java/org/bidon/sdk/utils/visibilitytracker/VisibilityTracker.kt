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
    private val isShown = AtomicBoolean(false)
    private val showTracked = AtomicBoolean(false)
    private val preDrawListener = ViewTreeObserver.OnPreDrawListener {
        checkVisible()
        true
    }
    private val requiredOnScreenTime get() = visibilityParams.timeThresholdMs
    private var shownObserverJob: Job? = null
    private var view: View? = null
    private var onViewShown: (() -> Unit)? = null
    private val attachStateChangeListener by lazy {
        object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}

            override fun onViewDetachedFromWindow(v: View) {
                stop()
            }
        }
    }

    fun start(
        view: View,
        onViewShown: () -> Unit,
    ) {
        if (isShown.get()) {
            return
        }
        if (isStarted.compareAndSet(/* expectedValue = */ false, /* newValue = */ true)) {
            this.onViewShown = onViewShown
            this.view = view
            logInfo(TAG, "Start tracking - $view")
            view.addOnAttachStateChangeListener(attachStateChangeListener)
            view.viewTreeObserver.addOnPreDrawListener(preDrawListener)
            checkVisible()
        }
    }

    fun stop() {
        logInfo(TAG, "Stop tracking - $view")
        view?.viewTreeObserver?.removeOnPreDrawListener(preDrawListener)
        view?.removeOnAttachStateChangeListener(attachStateChangeListener)
        shownObserverJob?.cancel()
        shownObserverJob = null
        view = null
        onViewShown = null
        isStarted.set(false)
    }

    private fun checkVisible() {
        if (showTracked.get()) return
        if (shownObserverJob?.isActive == true) return
        shownObserverJob?.cancel()
        shownObserverJob = scope.launch {
            pauseResumeObserver.lifecycleFlow.first { state ->
                (state == ActivityLifecycleState.Resumed).also {
                    if (!it) {
                        logInfo(TAG, "Paused. Application in background.")
                    }
                }
            }
            if (view.isOnTop(visibilityParams)) {
                delay(requiredOnScreenTime)
                if (view.isOnTop(visibilityParams)) {
                    if (showTracked.compareAndSet(false, true)) {
                        logInfo(TAG, "Tracked - $view")
                        isShown.set(true)
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
private const val TAG = "VisibilityTracker"
