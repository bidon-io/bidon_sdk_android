package com.appodealstack.bidon.core.impl

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.os.Bundle
import com.appodealstack.bidon.core.PauseResumeObserver
import com.appodealstack.bidon.core.ext.logInternal
import kotlinx.coroutines.flow.MutableStateFlow

internal class PauseResumeObserverImpl(
    application: Application
) : PauseResumeObserver {
    override val lifecycleFlow = MutableStateFlow(
        if (isForegrounded()) PauseResumeObserver.LifecycleState.Resumed else PauseResumeObserver.LifecycleState.Paused
    )

    init {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    lifecycleFlow.value = PauseResumeObserver.LifecycleState.Created
                }

                override fun onActivityStarted(activity: Activity) {
                    lifecycleFlow.value = PauseResumeObserver.LifecycleState.Started
                }

                override fun onActivityStopped(activity: Activity) {
                    lifecycleFlow.value = PauseResumeObserver.LifecycleState.Stopped
                }

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {
                    lifecycleFlow.value = PauseResumeObserver.LifecycleState.Destroyed
                }

                override fun onActivityResumed(activity: Activity) {
                    logInternal(Tag, "Activity Resumed")
                    lifecycleFlow.value = PauseResumeObserver.LifecycleState.Resumed
                }

                override fun onActivityPaused(activity: Activity) {
                    logInternal(Tag, "Activity Paused")
                    lifecycleFlow.value = PauseResumeObserver.LifecycleState.Paused
                }
            })
    }

    private fun isForegrounded(): Boolean {
        val appProcessInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(appProcessInfo)
        return appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND || appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
    }
}

private const val Tag = "PauseResumeObserver"