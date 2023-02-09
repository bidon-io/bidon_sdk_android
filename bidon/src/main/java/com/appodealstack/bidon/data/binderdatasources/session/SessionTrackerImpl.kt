package com.appodealstack.bidon.data.binderdatasources.session

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import com.appodealstack.bidon.data.time.ElapsedMonotonicTimeNow
import com.appodealstack.bidon.data.time.SystemTimeNow
import com.appodealstack.bidon.domain.stats.impl.logInfo
import com.appodealstack.bidon.view.helper.ActivityLifecycleState
import com.appodealstack.bidon.view.helper.PauseResumeObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
                        logInfo(Tag, "New session started with sessionId=$sessionId")
                    }
                }
                ActivityLifecycleState.Paused -> {
                    job?.cancel()
                    job = scope.launch {
                        delay(SessionResetTtlMs)
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
private const val SessionResetTtlMs = 30_000L

private const val Tag = "SessionTracker"