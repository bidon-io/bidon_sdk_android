package com.appodealstack.bidon.domain.common.usecases

import com.appodealstack.bidon.domain.stats.impl.logInternal
import com.appodealstack.bidon.view.helper.ActivityLifecycleState
import com.appodealstack.bidon.view.helper.SdkDispatchers
import com.appodealstack.bidon.view.helper.impl.ActivityLifecycleObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Timer stops if Application is in the background, and continues on the foregrounded.
 */
internal class CountDownTimer(
    private val activityLifecycleObserver: ActivityLifecycleObserver
) {
    private val scope: CoroutineScope by lazy { CoroutineScope(SdkDispatchers.Main) }
    private var timerDeferred: Deferred<Unit>? = null

    fun stop() {
        logInternal(Tag, "Canceled")
        timerDeferred?.cancel()
    }

    fun startTimer(timeoutMs: Long, onFinish: () -> Unit) {
        scope.launch {
            logInternal(Tag, "Started")
            val deferred = timerDeferred ?: async {
                val seconds = (timeoutMs / OneSecond).toInt()
                repeat(seconds) { second ->
                    delay(OneSecond)
                    activityLifecycleObserver.lifecycleFlow.first { state ->
                        state == ActivityLifecycleState.Resumed
                    }
                    logInternal(Tag, "Tick ${second + 1}/$seconds")
                }
            }.also { timerDeferred = it }
            try {
                deferred.await()
                onFinish()
                logInternal(Tag, "Finished")
            } finally {
                timerDeferred = null
            }
        }
    }
}

private const val OneSecond = 1000L
private const val Tag = "CountDownTimer"