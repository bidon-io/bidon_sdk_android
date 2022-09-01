package com.appodealstack.bidon.utilities.datasource.session

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import com.appodealstack.bidon.core.PauseResumeObserver
import com.appodealstack.bidon.core.PauseResumeObserver.LifecycleState
import com.appodealstack.bidon.core.ext.logInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.*

internal interface SessionTracker {
    val sessionId: String
    val launchTs: Long
    val launchMonotonicTs: Long
    val startTs: Long
    val startMonotonicTs: Long
    val ts: Long
    val monotonicTs: Long

    val memoryWarningsTs: List<Long>
    val memoryWarningsMonotonicTs: List<Long>
}

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

    private fun systemTime() = System.currentTimeMillis()
    private fun monotonicTime() = SystemClock.elapsedRealtime()

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
                LifecycleState.Resumed -> {
                    job?.cancel()
                    if (shouldStartNewSession) {
                        shouldStartNewSession = false
                        sessionId = UUID.randomUUID().toString()
                        startTs = systemTime()
                        startMonotonicTs = monotonicTime()
                        logInfo(Tag, "New session started with sessionId=$sessionId")
                    }
                }
                LifecycleState.Paused -> {
                    job?.cancel()
                    job = scope.launch {
                        delay(SessionResetTtlMs)
                        shouldStartNewSession = true
                    }
                }
                else -> Unit
            }
        }.launchIn(scope)
    }
}

/**
 * If application is paused for this time, then new session starts
 */
private const val SessionResetTtlMs = 30_000L

private const val Tag = "SessionTracker"