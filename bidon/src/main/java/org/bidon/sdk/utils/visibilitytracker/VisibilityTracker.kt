package org.bidon.sdk.utils.visibilitytracker

import android.view.View

internal interface VisibilityTracker {
    fun startTracking(
        view: View,
        visibilityParams: VisibilityParams,
        visibilityChangeCallback: VisibilityChangeCallback
    )

    fun stopTracking(view: View)
}