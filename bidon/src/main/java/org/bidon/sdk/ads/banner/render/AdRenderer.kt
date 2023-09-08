package org.bidon.sdk.ads.banner.render

import android.app.Activity
import android.graphics.Point
import android.graphics.PointF
import android.view.View
import org.bidon.sdk.ads.banner.BannerPosition
import org.bidon.sdk.ads.banner.BannerView

/**
 * Created by Aleksei Cherniaev on 05/09/2023.
 */
internal interface AdRenderer {
    fun render(
        activity: Activity,
        bannerView: BannerView,
        positionState: PositionState,
        animate: Boolean,
        handleConfigurationChanges: Boolean,
        renderListener: RenderListener
    ): Boolean

    fun hide()

    interface RenderListener {
        fun onRendered()
        fun onRenderFailed()
        fun onVisibilityIssued()
    }

    interface RenderInspector {
        fun isRenderPermitted(): Boolean
        fun isActivityValid(activity: Activity): Boolean
        fun isViewVisibleOnScreen(view: View?): Boolean
    }

    /**
     * Offset presents top and left offset in pixels.
     * Pivot presents pivot/anchor point in relative coordinates started from left/top corner.
     * @param pivot min value is 0f, max value is 1f
     * @param rotation in degrees
     */
    data class AdContainerParams(
        val offset: Point,
        val rotation: Int,
        val pivot: PointF
    )

    sealed interface PositionState {
        data class Place(val position: BannerPosition) : PositionState
        data class Coordinate(val adContainerParams: AdContainerParams) : PositionState

        companion object {
            val Default get() = Place(BannerPosition.HorizontalBottom)
        }
    }
}