package org.bidon.sdk.utils.visibilitytracker

//noinspection SuspiciousImport
import android.R
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import org.bidon.sdk.logs.logging.impl.logInfo
import kotlin.math.max
import kotlin.math.min

internal fun View?.isOnTop(visibilityParams: VisibilityParams): Boolean {
    if (this == null) return false
    val visibilityPercent = visibilityParams.pixelThreshold
    val maxCountOverlappedViews = visibilityParams.maxCountOverlappedViews
    val ignoreWindowFocus = visibilityParams.isIgnoreWindowFocus
    val ignoreOverlap = visibilityParams.isIgnoreOverlap
    val view = this
    val viewRect = Rect()
    if (!view.getGlobalVisibleRect(viewRect)) {
        logInfo(TAG, "Show wasn't tracked: global visibility verification failed - $view")
        return false
    }
    if (!view.isShown) {
        logInfo(TAG, "Show wasn't tracked: view visibility verification failed - $view")
        return false
    }
    if (view.isTransparent()) {
        logInfo(TAG, "Show wasn't tracked: view transparent verification failed - $view")
        return false
    }
    if (!ignoreWindowFocus && !view.hasWindowFocus()) {
        logInfo(TAG, "Show wasn't tracked: window focus verification failed - $view")
        return false
    }
    val totalAdViewArea = (view.width * view.height).toFloat()
    if (totalAdViewArea == 0.0f) {
        logInfo(TAG, "Show wasn't tracked: view size verification failed - $view")
        return false
    }
    val viewArea = viewRect.width() * viewRect.height()
    val percentOnScreen = viewArea / totalAdViewArea
    if (percentOnScreen < visibilityPercent) {
        logInfo(
            TAG,
            "Show wasn't tracked: ad view not completely visible ($percentOnScreen / $visibilityPercent) - $view"
        )
        return false
    }
    var content = view.parent as? View?
    while (content != null && content.id != R.id.content) {
        content = content.parent as View
    }
    if (content == null) {
        logInfo(TAG, "Show wasn't tracked: activity content layout not found - $view")
        return false
    }
    val rootViewRect = Rect()
    content.getGlobalVisibleRect(rootViewRect)
    if (!Rect.intersects(viewRect, rootViewRect)) {
        logInfo(TAG, "Show wasn't tracked: ad view is out of current window - $view")
        return false
    }
    if (!ignoreOverlap && view.hasOverlap(viewRect, visibilityPercent, maxCountOverlappedViews)) {
        // TODO Implement the task https://appodeal.atlassian.net/browse/BDN-551
    }
    return true
}

private fun View.isTransparent() = alpha == 0.0f

private fun View.getRectangle(): Rect {
    val location = IntArray(2)
    this.getLocationInWindow(location)
    return Rect(location[0], location[1], this.width + location[0], this.height + location[1])
}

private fun View.hasOverlap(
    viewRect: Rect,
    visibilityPercent: Float,
    maxCountOverlappedViews: Int
): Boolean {
    var view = this
    val rootView = view.rootView as? ViewGroup?
    var parent = view.parent as? ViewGroup?
    var countOverlappedViews = 0
    while (parent != null) {
        val index = parent.indexOfChild(view) + 1
        for (i in index until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child.visibility == View.VISIBLE) {
                val childRect: Rect = child.getRectangle()
                if (Rect.intersects(viewRect, childRect)) {
                    val visiblePercent = viewNotOverlappedAreaPercent(viewRect, childRect)
                    logInfo(
                        TAG,
                        "Show wasn't tracked: ad view is overlapped by another visible view ($child), visible percent: $visiblePercent / $visibilityPercent"
                    )
                    if (visiblePercent < visibilityPercent) {
                        logInfo(
                            TAG,
                            "Show wasn't tracked: ad view is covered by another view - $view"
                        )
                        return true
                    } else {
                        countOverlappedViews++
                        if (countOverlappedViews >= maxCountOverlappedViews) {
                            logInfo(
                                TAG,
                                "Show wasn't tracked: ad view is covered by too many views - $view"
                            )
                            return true
                        }
                    }
                }
            }
        }
        if (parent !== rootView) {
            view = parent
            parent = view.getParent() as ViewGroup
        } else {
            parent = null
        }
    }
    return false
}

private fun viewNotOverlappedAreaPercent(viewRect: Rect, coverRect: Rect): Float {
    val viewArea = viewRect.width() * viewRect.height()
    if (viewArea == 0) return 0f
    val minRight = min(viewRect.right, coverRect.right)
    val maxLeft = max(viewRect.left, coverRect.left)
    val minBottom = min(viewRect.bottom, coverRect.bottom)
    val maxTop = max(viewRect.top, coverRect.top)
    val xOverlap = max(0, minRight - maxLeft)
    val yOverlap = max(0, minBottom - maxTop)
    val overlapArea = xOverlap * yOverlap
    return (viewArea - overlapArea).toFloat() / viewArea
}

private const val TAG = "VisibilityTracker"
