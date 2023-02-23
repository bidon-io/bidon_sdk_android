package org.bidon.sdk.ads.banner.helper

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.bidon.sdk.ads.banner.helper.impl.ActivityLifecycleObserver
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.SdkDispatchers

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 *
 * Timer stops if Application is in the background, and continues on the foregrounded.
 */
internal class CountDownTimer(
    private val activityLifecycleObserver: ActivityLifecycleObserver
) {
    private val scope: CoroutineScope by lazy { CoroutineScope(SdkDispatchers.Main) }
    private var timerDeferred: Deferred<Unit>? = null

    fun stop() {
        logInfo(Tag, "Canceled")
        timerDeferred?.cancel()
    }

    fun startTimer(timeoutMs: Long, onFinish: () -> Unit) {
        scope.launch {
            logInfo(Tag, "Started")
            val deferred = timerDeferred ?: async {
                val seconds = (timeoutMs / OneSecond).toInt()
                repeat(seconds) { second ->
                    delay(OneSecond)
                    activityLifecycleObserver.lifecycleFlow.first { state ->
                        state == ActivityLifecycleState.Resumed
                    }
                    logInfo(Tag, "Tick ${second + 1}/$seconds")
                }
            }.also { timerDeferred = it }
            try {
                deferred.await()
                onFinish()
                logInfo(Tag, "Finished")
            } finally {
                timerDeferred = null
            }
        }
    }
}

private const val OneSecond = 1000L
private const val Tag = "CountDownTimer"