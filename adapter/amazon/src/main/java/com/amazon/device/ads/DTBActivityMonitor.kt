package com.amazon.device.ads

import android.app.Activity
import org.bidon.sdk.logs.logging.impl.logInfo

/**
 * Sets the activity to `ActivityMonitor` for correct impression tracking logic.
 * This class can be deleted after migrating Amazon SDK initialization to use an activity as the context.
 */
internal object DTBActivityMonitor {
    /**
     * Set activity to ActivityMonitor.
     *
     * @param activity Activity
     */
    fun setActivity(activity: Activity) {
        ActivityMonitor.getInstance()?.let { instance ->
            if (instance.currentActivity == null) {
                logInfo("DTBActivityMonitor", "Setting activity to DTBActivityMonitor")
                instance.onActivityResumed(activity)
            }
        }
    }
}