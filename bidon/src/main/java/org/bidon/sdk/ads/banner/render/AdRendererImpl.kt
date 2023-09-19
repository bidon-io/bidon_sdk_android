package org.bidon.sdk.ads.banner.render

import android.app.Activity
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.BannerPosition
import org.bidon.sdk.ads.banner.BannerView
import org.bidon.sdk.ads.banner.render.AdRenderer.PositionState
import org.bidon.sdk.ads.banner.render.ApplyInsetUseCase.applyWindowInsets
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.ext.TAG
import org.bidon.sdk.utils.ext.dp
import java.lang.ref.WeakReference

/**
 * Created by Aleksei Cherniaev on 05/09/2023.
 *
 * Hierarchy: (Network) AdView -> AdContainer -> RootContainer
 */
internal class AdRendererImpl(
    private val inspector: AdRenderer.RenderInspector,
    private val calculateAdContainerParams: CalculateAdContainerParamsUseCase
) : AdRenderer {

    private var activity = WeakReference<Activity>(null)
    private var safeAreaScreenSize = Point(0, 0)
    private val tag get() = TAG

    /**
     * RootContainer is the only one view for every [activity]
     */
    private var rootContainer: FrameLayout? = null

    /**
     * AdContainer changes with [positionState]
     */
    private var adContainer: FrameLayout? = null
    private var positionState: PositionState = PositionState.Place(BannerPosition.Default)
    private val lifecycleObserver by lazy { LifecycleObserver() }

    override fun render(
        activity: Activity,
        bannerView: BannerView,
        positionState: PositionState,
        animate: Boolean,
        handleConfigurationChanges: Boolean,
        renderListener: AdRenderer.RenderListener
    ) {
        observeActivity(activity)
        logInfo(tag, "Render banner $bannerView at $activity")
        logInfo(
            tag = tag,
            message = "--> AdContainer($adContainer), AdView($bannerView), $positionState, " +
                "${bannerView.format}, animate($animate), "
        )
        logInfo(tag, "${bannerView.adSize}. Obtained size: ${bannerView.obtainWidth()} x ${bannerView.obtainHeight()}")
        if (!inspector.isActivityValid(activity)) {
            hide(activity)
            renderListener.onRenderFailed()
            return
        }
        if (this.positionState != positionState) {
            logInfo(tag, "Position changed: ${this.positionState} -> $positionState")
            hide(activity)
        }
        if (inspector.isRenderPermitted()) {
            activity.runOnUiThread {
                this.positionState = positionState
                this.activity = WeakReference(activity)
                withRootContainer(activity) {
                    if (!bannerView.fits(positionState)) {
                        logInfo(tag, "Banner does not fit")
                        renderListener.onVisibilityIssued()
                        return@withRootContainer
                    }
                    if (!inspector.isViewVisibleOnScreen(view = adContainer)) {
                        createAdContainer(activity, positionState, bannerView)
                    }
                    bannerView.showAd()
                    adContainer?.addAdView(bannerView)
                    setAdViewsVisible(bannerView)
                    renderListener.onRendered()
                }
            }
        } else {
            renderListener.onRenderFailed()
        }
    }

    private fun BannerView.fits(positionState: PositionState): Boolean {
        if (positionState !is PositionState.Place) return true
        return when (positionState.position) {
            BannerPosition.HorizontalTop,
            BannerPosition.HorizontalBottom -> safeAreaScreenSize.x >= this.obtainWidth()

            BannerPosition.VerticalLeft,
            BannerPosition.VerticalRight -> safeAreaScreenSize.y >= this.obtainWidth()
        }
    }

    private fun withRootContainer(activity: Activity, onRootContainerReady: () -> Unit) {
        if (!inspector.isViewVisibleOnScreen(view = rootContainer) || activity != this.activity.get()) {
            createRootContainer(activity) {
                onRootContainerReady()
            }
        } else {
            onRootContainerReady()
        }
    }

    override fun hide(activity: Activity) {
        adContainer?.removeAllViews()
        adContainer = null
    }

    private fun setAdViewsVisible(adView: ViewGroup) {
        adView.visibility = View.VISIBLE
        adContainer?.visibility = View.VISIBLE
        rootContainer?.visibility = View.VISIBLE
        rootContainer?.bringToFront()
        adContainer?.bringToFront()
    }

    private fun createRootContainer(activity: Activity, onFinished: () -> Unit) {
        adContainer?.removeAllViews()
        rootContainer?.removeAllViews()
        val layoutParam = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        rootContainer = FrameLayout(activity).applyWindowInsets()
        activity.addContentView(rootContainer, layoutParam)
        rootContainer?.viewTreeObserver?.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    safeAreaScreenSize =
                        Point(rootContainer?.width ?: safeAreaScreenSize.x, rootContainer?.height ?: safeAreaScreenSize.y)
                    rootContainer?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                    onFinished()
                }
            }
        )
    }

    private fun createAdContainer(
        activity: Activity,
        positionState: PositionState,
        bannerView: BannerView
    ) {
        adContainer?.removeAllViews()
        rootContainer?.removeAllViews()
        val adContainer = FrameLayout(activity).also {
            this.adContainer = it
        }
        val (offset, rotation, anchor) = when (val state = positionState) {
            is PositionState.Coordinate -> state.adContainerParams
            is PositionState.Place -> calculateAdContainerParams(
                position = state.position,
                screenSize = safeAreaScreenSize,
                bannerHeight = bannerView.obtainHeight(),
            )
        }
        adContainer.setParams(
            offset = offset,
            pivot = anchor,
            rotation = rotation,
            width = bannerView.obtainWidth(),
            height = bannerView.obtainHeight()
        )
        rootContainer?.addView(adContainer, LayoutParams(bannerView.obtainWidth(), bannerView.obtainHeight()))
    }

    private fun FrameLayout.setParams(offset: Point, pivot: PointF, rotation: Int, width: Int, height: Int) {
        val translatedX = offset.x - pivot.x * width
        val translatedY = offset.y - pivot.y * height
        this.pivotX = width * pivot.x
        this.pivotY = height * pivot.y
        this.rotation = rotation.toFloat()
        this.x = translatedX
        this.y = translatedY
    }

    private fun FrameLayout.addAdView(bannerView: BannerView) {
        val adContainer: FrameLayout = this
        val oldAdView = adContainer.getChildAt(0)
        val isViewsTheSame = oldAdView == bannerView
        if (isViewsTheSame) {
            logInfo(this@AdRendererImpl.tag, "View and position does not changed")
            return
        }
        bannerView.parent?.let {
            (it as ViewGroup).removeView(bannerView)
        }
        adContainer.setBackgroundColor(Color.TRANSPARENT)
        adContainer.addView(bannerView, LayoutParams(bannerView.obtainWidth(), bannerView.obtainHeight(), Gravity.CENTER))
        oldAdView?.animate()
            ?.alpha(0.0f)
            ?.setDuration(800)
            ?.withLayer()
            ?.withStartAction { oldAdView.bringToFront() }
            ?.withEndAction { adContainer.removeView(oldAdView) }
            ?.start()
    }

    private fun observeActivity(activity: Activity) {
        lifecycleObserver.observe(
            applicationContext = activity.applicationContext,
            onActivityDestroyed = { destroyedActivity ->
                logInfo(tag, "Activity destroyed: $destroyedActivity")
                if (this@AdRendererImpl.activity.get() == destroyedActivity) {
                    hide(activity)
                    rootContainer?.removeAllViews()
                    rootContainer = null
                    this@AdRendererImpl.activity = WeakReference(null)
                }
            }
        )
    }

    private fun BannerView.obtainWidth() = this.adSize?.widthDp?.dp ?: when (format) {
        BannerFormat.MRec -> 300.dp
        BannerFormat.LeaderBoard -> 728.dp
        BannerFormat.Banner -> 320.dp
        BannerFormat.Adaptive -> WRAP_CONTENT
    }

    private fun BannerView.obtainHeight() = this.adSize?.heightDp?.dp ?: when (format) {
        BannerFormat.MRec -> 250.dp
        BannerFormat.LeaderBoard -> 90.dp
        BannerFormat.Banner -> 50.dp
        BannerFormat.Adaptive -> WRAP_CONTENT
    }
}