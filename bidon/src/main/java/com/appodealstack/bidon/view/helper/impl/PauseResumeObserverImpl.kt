package com.appodealstack.bidon.view.helper.impl

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.os.Bundle
import com.appodealstack.bidon.domain.logging.impl.logInfo
import com.appodealstack.bidon.view.helper.ActivityLifecycleState
import com.appodealstack.bidon.view.helper.PauseResumeObserver
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.ref.WeakReference
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class PauseResumeObserverImpl(
    application: Application
) : PauseResumeObserver {

    private var weakActivity: WeakReference<Activity>? = null

    override val lifecycleFlow = MutableStateFlow(
        if (isForegrounded()) ActivityLifecycleState.Resumed else ActivityLifecycleState.Paused
    )

    init {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {
                    logInfo(Tag, "Activity Destroyed (current: ${weakActivity?.get()}) $activity")
                    if (activity == weakActivity?.get()) {
                        weakActivity = null
                    }
                }

                override fun onActivityResumed(activity: Activity) {
                    logInfo(Tag, "Activity Resumed (current: ${weakActivity?.get()}) $activity")
                    weakActivity = WeakReference(activity)
                    lifecycleFlow.value = ActivityLifecycleState.Resumed
                }

                override fun onActivityPaused(activity: Activity) {
                    logInfo(Tag, "Activity <Paused> (current: ${weakActivity?.get()}) $activity")
                    if (activity == weakActivity?.get()) {
                        logInfo(Tag, "Activity Paused $activity")
                        lifecycleFlow.value = ActivityLifecycleState.Paused
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
