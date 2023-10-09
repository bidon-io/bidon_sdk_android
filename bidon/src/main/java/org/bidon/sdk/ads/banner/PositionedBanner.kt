package org.bidon.sdk.ads.banner

import android.app.Activity
import android.graphics.Point
import android.graphics.PointF
import org.bidon.sdk.BidonSdk

/**
 * Created by Aleksei Cherniaev on 04/09/2023.
 *
 * [showAd], [hideAd], [destroyAd], [notifyLoss] need activity to be passed as parameter, mainly for Unity UI thread.
 */
interface PositionedBanner {
    /**
     * Common interface for [BannerView]
     */
    val adSize: AdSize?

    /**
     * Shows if banner is displaying
     */
    val isDisplaying: Boolean

    /**
     * Predefined [BannerFormat].
     */
    val bannerFormat: BannerFormat

    /**
     * Predefined [BannerPosition].
     * Always uses safe area insets.
     */
    fun setPosition(position: BannerPosition)

    /**
     * Offset presents top and left offset in pixels.
     * Anchor point presents pivot point in relative coordinates started from left/top corner.
     * @param offset in physical pixels
     * @param rotation in degrees
     * @param anchor min value is 0f, max value is 1f
     */
    fun setCustomPosition(
        offset: Point,
        rotation: Int,
        anchor: PointF
    )

    fun setBannerFormat(bannerFormat: BannerFormat)
    fun loadAd(activity: Activity, pricefloor: Double = BidonSdk.DefaultPricefloor)

    /**
     * Shows if banner is ready to show
     */
    fun isReady(): Boolean
    fun showAd(activity: Activity)
    fun hideAd(activity: Activity)
    fun destroyAd(activity: Activity)
    fun setBannerListener(listener: BannerListener?)

    fun notifyLoss(activity: Activity, winnerDemandId: String, winnerEcpm: Double)
    fun notifyWin()
}