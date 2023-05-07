package org.bidon.sdk.ads.banner.helper

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Created by Aleksei Cherniaev on 07/05/2023.
 */
object DeviceType {
    var isTablet: Boolean = false
        private set

    /**
     * Only visual Contexts (such as Activity or one created with Context#createWindowContext)
     * or ones created with Context#createDisplayContext are associated with displays.
     */
    fun init(activity: Activity) {
        val display: Display = getDisplay(activity) ?: return
        val metrics = DisplayMetrics()
        val realSize = Point()
        display.getRealSize(realSize)
        display.getMetrics(metrics)
        val width = (realSize.x / metrics.xdpi).toDouble().pow(2.0)
        val height = (realSize.y / metrics.ydpi).toDouble().pow(2.0)
        val screenInches = sqrt(width + height)
        isTablet = (screenInches > 7.0)
    }

    private fun getDisplay(context: Context): Display? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay
        }
    }
}