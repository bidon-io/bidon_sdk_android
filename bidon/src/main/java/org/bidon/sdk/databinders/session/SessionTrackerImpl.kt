package org.bidon.sdk.databinders.session

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.bidon.sdk.ads.banner.helper.ActivityLifecycleState
import org.bidon.sdk.ads.banner.helper.PauseResumeObserver
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.ext.ElapsedMonotonicTimeNow
import org.bidon.sdk.utils.ext.SystemTimeNow
import java.util.*

internal class SessionTrackerImpl(
    pauseResumeObserver: PauseResumeObserver,
    context: Context
) : SessionTracker {

    @Suppress("OPT_IN_USAGE")
    private val scope by lazy {
        CoroutineScope(newSingleThreadContext("SessionTracker"))
    }

    init {
        observeSessionTime(pauseResumeObserver)
        observeMemoryIssue(context)
    }

    override var sessionId: String = UUID.randomUUID().toString()

    override val launchTs: Long = systemTime()
    override val launchMonotonicTs: Long = monotonicTime()

    override var startTs: Long = systemTime()
    override var startMonotonicTs: Long = monotonicTime()

    override val ts: Long get() = systemTime()
    override val monotonicTs: Long get() = monotonicTime()

    override val memoryWarningsTs = mutableListOf<Long>()
    override val memoryWarningsMonotonicTs = mutableListOf<Long>()

    private fun systemTime() = SystemTimeNow
    private fun monotonicTime() = ElapsedMonotonicTimeNow

    private fun observeMemoryIssue(context: Context) {
        (context.applicationContext as Application).registerComponentCallbacks(
            object : ComponentCallbacks2 {
                override fun onConfigurationChanged(newConfig: Configuration) {}
                override fun onTrimMemory(level: Int) {}

                override fun onLowMemory() {
                    memoryWarningsTs.add(systemTime())
                    memoryWarningsMonotonicTs.add(monotonicTime())
                }
            }
        )
    }

    private fun observeSessionTime(pauseResumeObserver: PauseResumeObserver) {
        var job: Job? = null
        var shouldStartNewSession = false
        pauseResumeObserver.lifecycleFlow.onEach { state ->
            when (state) {
                ActivityLifecycleState.Resumed -> {
                    job?.cancel()
                    if (shouldStartNewSession) {
                        shouldStartNewSession = false
                        sessionId = UUID.randomUUID().toString()
                        startTs = systemTime()
                        startMonotonicTs = monotonicTime()
                        logInfo(TAG, "New session started with sessionId=$sessionId")
                    }
                }
                ActivityLifecycleState.Paused -> {
                    job?.cancel()
                    job = scope.launch {
                        delay(SessionTtlMs)
                        shouldStartNewSession = true
                    }
                }
            }
        }.launchIn(scope)
    }
}

/**
 * If application is paused for this time, then new session starts
 */
private const val SessionTtlMs = 30_000L

private const val TAG = "SessionTracker"