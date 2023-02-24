package org.bidon.sdk.ads.banner.helper.impl

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import org.bidon.sdk.ads.banner.helper.ActivityLifecycleState
import org.bidon.sdk.ads.banner.helper.PauseResumeObserver
import org.bidon.sdk.logs.logging.impl.logInfo
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
        logInfo(Tag, "Activity initial state $state: $activity")
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
                        logInfo(Tag, "Activity Destroyed $activity")
                        weakActivity = null
                    }
                }

                override fun onActivityResumed(activity: Activity) {
                    if (activity == weakActivity?.get()) {
                        logInfo(Tag, "Activity Resumed $activity")
                        lifecycleFlow.value = ActivityLifecycleState.Resumed
                    }
                }

                override fun onActivityPaused(activity: Activity) {
                    if (activity == weakActivity?.get()) {
                        logInfo(Tag, "Activity Paused $activity")
                        lifecycleFlow.value = ActivityLifecycleState.Paused
                    }
                }
            }
        )
    }
}

private const val Tag = "ActivityLifecycleObserver"
