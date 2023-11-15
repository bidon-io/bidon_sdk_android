package org.bidon.admob.ext

import android.content.Context
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import org.bidon.admob.BuildConfig
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo.isTablet

internal var adapterVersion = BuildConfig.ADAPTER_VERSION
internal var sdkVersion = MobileAds.getVersion().toString()

internal fun BannerFormat.toAdmobAdSize(
    context: Context,
    containerWidth: Float
): AdSize {
    return when (this) {
        BannerFormat.Banner -> AdSize.BANNER
        BannerFormat.LeaderBoard -> AdSize.LEADERBOARD
        BannerFormat.MRec -> AdSize.MEDIUM_RECTANGLE
        BannerFormat.Adaptive -> {
            if (isTablet) {
                AdSize.LEADERBOARD
            } else {
                AdSize.BANNER
            }
            // TODO(): Fix this
            // val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            // val display = windowManager.defaultDisplay
            // val outMetrics = DisplayMetrics()
            // display.getMetrics(outMetrics)
            // val density = outMetrics.density
            // var adWidthPixels = containerWidth
            // if (adWidthPixels == 0f) {
            //     adWidthPixels = outMetrics.widthPixels.toFloat()
            // }
            // val adWidth = (adWidthPixels / density).toInt()
            // AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
        }
    }
}
