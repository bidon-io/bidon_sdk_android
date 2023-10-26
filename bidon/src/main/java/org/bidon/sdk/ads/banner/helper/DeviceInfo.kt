package org.bidon.sdk.ads.banner.helper

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Created by Aleksei Cherniaev on 07/05/2023.
 */
object DeviceInfo {

    private var applicationContext: Context? = null

    var isTablet: Boolean = false
        private set

    val isLandscapeConfiguration: Boolean
        get() = applicationContext?.resources?.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE

    val screenWidthDp: Int
        get() {
            return runCatching {
                val displayMetrics = applicationContext?.resources?.displayMetrics ?: return 0
                val px = displayMetrics.widthPixels
                return (px / displayMetrics.density).toInt()
            }.getOrNull() ?: 0
        }

    val screenHeightDp: Int
        get() {
            return runCatching {
                val displayMetrics = applicationContext?.resources?.displayMetrics ?: return 0
                val px = displayMetrics.heightPixels
                return (px / displayMetrics.density).toInt()
            }.getOrNull() ?: 0
        }

    /**
     * Only visual Contexts (such as Activity or one created with Context#createWindowContext)
     * or ones created with Context#createDisplayContext are associated with displays.
     */
    @Suppress("DEPRECATION")
    fun init(context: Context) {
        applicationContext = context.applicationContext
        val display: Display = getDisplay(context) ?: return
        val metrics = DisplayMetrics()
        val realSize = Point()
        display.getRealSize(realSize)
        display.getMetrics(metrics)
        val width = (realSize.x / metrics.xdpi).toDouble().pow(2.0)
        val height = (realSize.y / metrics.ydpi).toDouble().pow(2.0)
        val screenInches = sqrt(width + height)
        isTablet = (screenInches > 7.0)
    }

    @Suppress("DEPRECATION")
    private fun getDisplay(context: Context): Display? {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return windowManager.defaultDisplay
    }
}