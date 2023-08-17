package org.bidon.admob

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import com.google.android.gms.ads.AdSize
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdapterParameters
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.models.LineItem

object AdmobInitParameters : AdapterParameters

class AdmobBannerAuctionParams(
    val context: Context,
    val bannerFormat: BannerFormat,
    val lineItem: LineItem,
    val containerWidth: Float,
    override val adUnitId: String
) : AdAuctionParams {
    val adSize: AdSize
        get() = when (bannerFormat) {
            BannerFormat.Banner -> AdSize.BANNER
            BannerFormat.LeaderBoard -> AdSize.LEADERBOARD
            BannerFormat.MRec -> AdSize.MEDIUM_RECTANGLE
            BannerFormat.Adaptive -> context.adaptiveAdSize(containerWidth)
        }

    override val price: Double get() = lineItem.pricefloor

    override fun toString(): String {
        return "AdmobBannerAuctionParams($lineItem)"
    }

    @Suppress("DEPRECATION")
    private fun Context.adaptiveAdSize(width: Float): AdSize {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val density = outMetrics.density
        var adWidthPixels = width
        if (adWidthPixels == 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }
        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }
}

class AdmobFullscreenAdAuctionParams(
    val context: Context,
    val lineItem: LineItem,
    override val adUnitId: String
) : AdAuctionParams {

    override val price: Double get() = lineItem.pricefloor

    override fun toString(): String {
        return "AdmobFullscreenAdAuctionParams(lineItem=$lineItem)"
    }
}
