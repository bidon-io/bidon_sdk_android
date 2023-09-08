package org.bidon.sdk.ads.banner.render

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.WindowInsets
import android.widget.FrameLayout
import kotlin.math.max

internal open class SafeInsetsFrameLayout @JvmOverloads constructor(
    context: Context,
    private val useSafeArea: Boolean = true
) : FrameLayout(context) {

    private val tmpRect = Rect()

    init {
        fitsSystemWindows = true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            requestApplyInsets()
        }
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        if (!useSafeArea) {
            return insets
        }
        val targetRect: Rect
        // If we have systemWindowInsets, than we should apply Cutout insets
        if (insets.systemWindowInsetLeft != 0 || insets.systemWindowInsetTop != 0 ||
            insets.systemWindowInsetRight != 0 || insets.systemWindowInsetBottom != 0
        ) {
            fillSafeInsets(insets, tmpRect)
            targetRect = tmpRect
            if (adjustInsetsCenter()) {
                tmpRect.right = max(tmpRect.left, tmpRect.right)
                tmpRect.left = tmpRect.right
            }
        } else {
            targetRect = EMPTY_RECT
        }
        fitSystemWindows(targetRect)
        return insets
    }

    private fun fillSafeInsets(windowInsets: WindowInsets, rect: Rect) {
        rect.setEmpty()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return
        }
        val displayCutout = windowInsets.displayCutout ?: return
        rect[displayCutout.safeInsetLeft, displayCutout.safeInsetTop, displayCutout.safeInsetRight] =
            displayCutout.safeInsetBottom
    }

    protected open fun adjustInsetsCenter(): Boolean {
        return true
    }

    companion object {
        private val EMPTY_RECT = Rect()
    }
}