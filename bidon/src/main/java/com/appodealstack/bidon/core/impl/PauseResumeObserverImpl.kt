package com.appodealstack.bidon.core.impl

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.os.Bundle
import com.appodealstack.bidon.core.PauseResumeObserver
import com.appodealstack.bidon.core.ext.logInternal
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.ref.WeakReference

internal class PauseResumeObserverImpl(
    application: Application
) : PauseResumeObserver {

    private var weakActivity: WeakReference<Activity>? = null

    override val lifecycleFlow = MutableStateFlow(
        if (isForegrounded()) PauseResumeObserver.LifecycleState.Resumed else PauseResumeObserver.LifecycleState.Paused
    )

    init {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {
                    if (activity == weakActivity?.get()) {
                        weakActivity = null
                    }
                }

                override fun onActivityResumed(activity: Activity) {
                    logInternal(Tag, "Activity Resumed $activity")
                    weakActivity = WeakReference(activity)
                    lifecycleFlow.value = PauseResumeObserver.LifecycleState.Resumed
                }

                override fun onActivityPaused(activity: Activity) {
                    if (activity == weakActivity?.get()) {
                        logInternal(Tag, "Activity Paused $activity")
                        lifecycleFlow.value = PauseResumeObserver.LifecycleState.Paused
                    }
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