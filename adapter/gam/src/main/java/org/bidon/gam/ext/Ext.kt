package org.bidon.gam.ext

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.Display
import android.view.WindowManager
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import org.bidon.gam.BuildConfig
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo.isTablet
import kotlin.math.roundToInt

internal var adapterVersion = BuildConfig.ADAPTER_VERSION
internal var sdkVersion = MobileAds.getVersion().toString()

internal fun BannerFormat.toGamAdSize(
    context: Context,
    containerWidth: Float,
): AdSize {
    return when (this) {
        BannerFormat.Banner -> AdSize.BANNER
        BannerFormat.LeaderBoard -> AdSize.LEADERBOARD
        BannerFormat.MRec -> AdSize.MEDIUM_RECTANGLE
        BannerFormat.Adaptive -> {
            if (isTablet) {
                AdSize.LEADERBOARD
            } else {
//                val adWidth = getScreenWidth(context)
//                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
                AdSize.BANNER
            }
        }
    }
}

private fun getScreenWidth(context: Context): Int {
    val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display
    } else {
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    }
    val displayMetrics = context.resources.displayMetrics
    val size = Point()
    display?.getSize(size)
    return (size.x / displayMetrics.density).roundToInt()
}