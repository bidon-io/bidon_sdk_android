package com.appodealstack.bidon.view

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
import android.app.Application
import android.os.Bundle
import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.view.PauseResumeObserver.LifecycleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface PauseResumeObserver {
    val lifecycleFlow: StateFlow<LifecycleState>

    enum class LifecycleState {
        Created,
        Started,
        Resumed,
        Paused,
        Stopped,
        Destroyed,
    }
}

internal class PauseResumeObserverImpl(
    application: Application
) : PauseResumeObserver {
    override val lifecycleFlow = MutableStateFlow(
        if (isForegrounded()) LifecycleState.Resumed else LifecycleState.Paused
    )

    init {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    lifecycleFlow.value = LifecycleState.Created
                }

                override fun onActivityStarted(activity: Activity) {
                    lifecycleFlow.value = LifecycleState.Started
                }

                override fun onActivityStopped(activity: Activity) {
                    lifecycleFlow.value = LifecycleState.Stopped
                }

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {
                    lifecycleFlow.value = LifecycleState.Destroyed
                }

                override fun onActivityResumed(activity: Activity) {
                    logInternal(Tag, "Activity Resumed")
                    lifecycleFlow.value = LifecycleState.Resumed
                }

                override fun onActivityPaused(activity: Activity) {
                    logInternal(Tag, "Activity Paused")
                    lifecycleFlow.value = LifecycleState.Paused
                }
            })
    }


    private fun isForegrounded(): Boolean {
        val appProcessInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(appProcessInfo)
        return appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE
    }
}

private const val Tag = "PauseResumeObserver"