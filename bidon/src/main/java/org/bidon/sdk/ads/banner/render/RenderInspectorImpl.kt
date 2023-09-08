package org.bidon.sdk.ads.banner.render

import android.app.Activity
import android.content.res.Resources
import android.graphics.Rect
import android.view.View

/**
 * Created by Aleksei Cherniaev on 24/04/2023.
 */
internal class RenderInspectorImpl : AdRenderer.RenderInspector {

    override fun isRenderPermitted(): Boolean {
        // TODO("Not yet implemented")
        return true
    }

    override fun isActivityValid(activity: Activity): Boolean {
        return !activity.isDestroyed && !activity.isFinishing
    }

    override fun isViewVisibleOnScreen(view: View?): Boolean {
        if (view == null) {
            return false
        }
        if (!view.isShown) {
            return false
        }
        val actualPosition = Rect()
        val isGlobalVisible = view.getGlobalVisibleRect(actualPosition)
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        val screen = Rect(0, 0, screenWidth, screenHeight)
        return isGlobalVisible && Rect.intersects(actualPosition, screen)
    }
}