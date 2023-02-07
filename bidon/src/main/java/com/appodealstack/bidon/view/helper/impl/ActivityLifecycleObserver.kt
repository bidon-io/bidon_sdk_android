package com.appodealstack.bidon.view.helper.impl

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.appodealstack.bidon.domain.stats.impl.logInternal
import com.appodealstack.bidon.view.helper.ActivityLifecycleState
import com.appodealstack.bidon.view.helper.PauseResumeObserver
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.ref.WeakReference
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class ActivityLifecycleObserver(activity: Activity) : PauseResumeObserver {

    override val lifecycleFlow = MutableStateFlow(getInitialState(activity))

    private var weakActivity: WeakReference<Activity>? = WeakReference(activity)

    private fun getInitialState(activity: Activity): ActivityLifecycleState {
        registerApplicationObserver(activity.application)
        val state = ActivityLifecycleState.Resumed.takeIf {
            activity.window.decorView.rootView.isShown
        } ?: ActivityLifecycleState.Paused
        logInternal(Tag, "Activity initial state $state: $activity")
        return state
    }

    private fun registerApplicationObserver(application: Application) {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {
                    if (activity == weakActivity?.get()) {
                        logInternal(Tag, "Activity Destroyed $activity")
                        weakActivity = null
                    }
                }

                override fun onActivityResumed(activity: Activity) {
                    if (activity == weakActivity?.get()) {
                        logInternal(Tag, "Activity Resumed $activity")
                        lifecycleFlow.value = ActivityLifecycleState.Resumed
                    }
                }

                override fun onActivityPaused(activity: Activity) {
                    if (activity == weakActivity?.get()) {
                        logInternal(Tag, "Activity Paused $activity")
                        lifecycleFlow.value = ActivityLifecycleState.Paused
                    }
                }
            }
        )
    }
}

private const val Tag = "ActivityLifecycleObserver"
