package org.bidon.sdk.ads.banner.render

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Aleksei Cherniaev on 21/04/2023.
 */
internal class LifecycleObserver {
    private val isLifecycleRegistered = AtomicBoolean(false)

    fun observe(applicationContext: Context, onActivityDestroyed: (Activity) -> Unit) {
        if (!isLifecycleRegistered.getAndSet(true)) {
            applicationContext.registerComponentCallbacks(object : ComponentCallbacks {
                override fun onConfigurationChanged(newConfig: Configuration) {
                    // TODO implement configuration changes logic
                }

                override fun onLowMemory() {}
            })
            (applicationContext as Application).registerActivityLifecycleCallbacks(object :
                    Application.ActivityLifecycleCallbacks {
                    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                    override fun onActivityStarted(activity: Activity) {}
                    override fun onActivityResumed(activity: Activity) {}
                    override fun onActivityPaused(activity: Activity) {}
                    override fun onActivityStopped(activity: Activity) {}
                    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                    override fun onActivityDestroyed(activity: Activity) {
                        onActivityDestroyed.invoke(activity)
                    }
                })
        }
    }
}