package com.appodealstack.bidon.auctions.domain

import com.appodealstack.bidon.core.PauseResumeObserver
import com.appodealstack.bidon.core.SdkDispatchers
import com.appodealstack.bidon.core.ext.logInternal
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Timer stops if Application is in the background, and continues on the foregrounded.
 */
internal class CountDownTimer(
    private val pauseResumeObserver: PauseResumeObserver
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
                    pauseResumeObserver.lifecycleFlow.first { state ->
                        state == PauseResumeObserver.LifecycleState.Resumed
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