package org.bidon.sdk.ads.banner.render

import android.app.Activity
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import org.bidon.sdk.ads.banner.BannerPosition
import org.bidon.sdk.ads.banner.BannerView
import org.bidon.sdk.ads.banner.render.AdRenderer.PositionState
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.ext.TAG
import org.bidon.sdk.utils.ext.dpToPx
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
     * RootContainer is the only one view for every [activity].
     * Implements insets and contains [adContainer] and [BannerView].
     */
    private var rootContainer: RootAdContainer? = null

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
        logInfo(tag, "Render banner $bannerView at $activity. ${Thread.currentThread()}")
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
            this.positionState = positionState
            this.activity = WeakReference(activity)
            withRootContainer(activity) {
                if (!bannerView.fits(positionState)) {
                    logInfo(tag, "Banner does not fit")
                    renderListener.onVisibilityIssued()
                    return@withRootContainer
                }
                val params = calculateAdContainerParams(
                    positionState = positionState,
                    screenSize = safeAreaScreenSize,
                    bannerWidth = bannerView.obtainWidth(),
                    bannerHeight = bannerView.obtainHeight(),
                )
                if (!inspector.isViewVisibleOnScreen(view = adContainer)) {
                    createAdContainer(activity, params)
                }
                bannerView.rotation = params.baseParams.rotation.toFloat()
                bannerView.showAd()
                adContainer?.addAdView(bannerView)
                setAdViewsVisible(bannerView)
                renderListener.onRendered()
            }
        } else {
            renderListener.onRenderFailed()
        }
    }

    override fun hide(activity: Activity) {
        adContainer?.removeAllViews()
        adContainer = null
    }

    override fun destroy(activity: Activity) {
        hide(activity)
        rootContainer?.clearRootContainer()
        rootContainer = null
        this.activity = WeakReference(null)
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

    private fun setAdViewsVisible(adView: ViewGroup) {
        adView.visibility = View.VISIBLE
        adContainer?.visibility = View.VISIBLE
        rootContainer?.visibility = View.VISIBLE
        rootContainer?.bringToFront()
        adContainer?.bringToFront()
    }

    private fun createRootContainer(activity: Activity, onFinished: () -> Unit) {
        adContainer?.removeAllViews()
        rootContainer?.clearRootContainer()
        val layoutParam = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        rootContainer = RootAdContainer(activity)
        activity.addContentView(rootContainer, layoutParam)
        rootContainer?.obtainSize { safeAreaScreenSize ->
            this.safeAreaScreenSize = safeAreaScreenSize
            onFinished()
        }
    }

    private fun createAdContainer(
        activity: Activity,
        params: AdViewsParameters
    ) {
        adContainer?.removeAllViews()
        rootContainer?.removeAllViews()
        val adContainer = FrameLayout(activity).also {
            this.adContainer = it
        }
        adContainer.setParams(
            offset = params.baseParams.offset,
            pivot = params.baseParams.pivot,
            width = params.adContainerWidth,
            height = params.adContainerHeight
        )
        rootContainer?.addView(
            adContainer,
            LayoutParams(params.adContainerLayoutParamsWidth, params.adContainerLayoutParamsHeight)
        )
    }

    private fun FrameLayout.setParams(offset: Point, pivot: PointF, width: Int, height: Int) {
        val translatedX = offset.x - pivot.x * width
        val translatedY = offset.y - pivot.y * height
        this.pivotX = width * pivot.x
        this.pivotY = height * pivot.y
        this.x = translatedX
        this.y = translatedY
    }

    private fun FrameLayout.addAdView(bannerView: BannerView) {
        bannerView.clipChildren = false
        bannerView.clipToPadding = false
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

    private fun BannerView.obtainWidth() = this.adSize.widthDp.dpToPx

    private fun BannerView.obtainHeight() = this.adSize.heightDp.dpToPx
}