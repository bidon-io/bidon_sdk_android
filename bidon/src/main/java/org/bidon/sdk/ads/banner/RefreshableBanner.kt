package org.bidon.sdk.ads.banner

import android.app.Activity
import android.graphics.Point
import android.graphics.PointF
import androidx.annotation.Keep
import androidx.core.view.children
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.ext.ad
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.banner.helper.ActivityLifecycleState
import org.bidon.sdk.ads.banner.helper.PauseResumeObserver
import org.bidon.sdk.ads.banner.helper.getWidthDp
import org.bidon.sdk.ads.banner.render.AdRenderer
import org.bidon.sdk.ads.banner.render.AdRenderer.PositionState
import org.bidon.sdk.ads.cache.AdCache
import org.bidon.sdk.ads.cache.Cacheable
import org.bidon.sdk.ads.cache.Refreshable
import org.bidon.sdk.ads.cache.Refreshable.Companion.DefaultRefreshTimeout
import org.bidon.sdk.ads.cache.Refresher
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.ext.TAG
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Aleksei Cherniaev on 05/09/2023.
 */
@Keep
class RefreshableBanner private constructor(
    override val bannerFormat: BannerFormat,
    private val adCache: AdCache,
    private val refresher: Refresher,
    private val pauseResumeObserver: PauseResumeObserver,
    private val extras: Extras = adCache.demandAd,
    private val demandAd: DemandAd = adCache.demandAd,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
) : PositionedBanner,
    Refreshable,
    Cacheable,
    Extras by extras {

    constructor(bannerFormat: BannerFormat) : this(
        bannerFormat = bannerFormat,
        adCache = get<AdCache> {
            params(DemandAd(AdType.Banner))
        },
        refresher = get(),
        pauseResumeObserver = get()
    ) {
        logInfo(tag, "Created $this")
    }

    private val tag get() = TAG
    private var weakActivity = WeakReference<Activity>(null)
    private val getActivity: Activity?
        get() = weakActivity.get()?.takeIf { !it.isDestroyed && !it.isFinishing }

    private var refreshTimeout: Long = DefaultRefreshTimeout
    private val displaying = MutableStateFlow(false)
    private var placement: String = "default"
    private var currentBannerView: BannerView2? = null
    private val showAfterLoad = AtomicBoolean(false)
    private var positionState: PositionState = PositionState.Default
    private var publisherListener: BannerListener? = null
    private val adRenderer: AdRenderer by lazy { get() }
    private var displayingJob: Job? = null

    private val listener = object : BannerListener {
        override fun onAdLoaded(ad: Ad) {
            publisherListener?.onAdLoaded(ad)
        }

        override fun onAdLoadFailed(cause: BidonError) {
            publisherListener?.onAdLoadFailed(cause)
        }

        override fun onAdShown(ad: Ad) {
            publisherListener?.onAdShown(ad)
        }

        override fun onAdClicked(ad: Ad) {
            publisherListener?.onAdClicked(ad)
        }

        override fun onAdExpired(ad: Ad) {
            publisherListener?.onAdExpired(ad)
        }

        override fun onRevenuePaid(ad: Ad, adValue: AdValue) {
            publisherListener?.onRevenuePaid(ad, adValue)
        }

        override fun onAdShowFailed(cause: BidonError) {
            publisherListener?.onAdShowFailed(cause)
        }
    }

    override val adSize: AdSize?
        get() = currentBannerView?.adSize

    override var isDisplaying: Boolean = false
        private set

    override fun setRefreshTimeout(timeoutMs: Long) {
        this.refreshTimeout = timeoutMs
    }

    /**
     * Positioning functions
     */
    override fun setPosition(position: BannerPosition) {
        logInfo(tag, "Set position $position")
        positionState = PositionState.Place(position)
    }

    override fun setCustomPosition(offset: Point, rotation: Int, anchor: PointF) {
        logInfo(tag, "Set position by coordinates Offset($offset), Rotation($rotation), Anchor($anchor)")
        positionState = PositionState.Coordinate(
            AdRenderer.AdContainerParams(offset, rotation, anchor)
        )
    }

    /**
     * BannerView's functions
     */
    override fun setBannerFormat(bannerFormat: BannerFormat) {
        logError(tag, "Banner format can't be changed after initialization", IllegalStateException())
    }

    override fun loadAd(activity: Activity, pricefloor: Double) {
        if (!BidonSdk.isInitialized()) {
            listener.onAdLoadFailed(BidonError.SdkNotInitialized)
            return
        }
        adCache.cache(
            adTypeParam = AdTypeParam.Banner(
                activity = activity,
                pricefloor = pricefloor,
                bannerFormat = bannerFormat,
                containerWidth = bannerFormat.getWidthDp().toFloat()
            ),
            onEach = { auctionResult ->
                logInfo(tag, "Ad is loaded and available to show.")
                auctionResult.adSource.ad?.let { listener.onAdLoaded(it) }
            }
        )
    }

    override fun isReady(): Boolean = currentBannerView?.isReady() == true

    fun showAd(activity: Activity, placement: String) {
        logInfo(tag, "Show ad")
        if (!BidonSdk.isInitialized()) {
            publisherListener?.onAdLoadFailed(BidonError.SdkNotInitialized)
            return
        }
        this.placement = placement
        this.weakActivity = WeakReference(activity)
        this.displaying.value = true
        if (currentBannerView != null) {
            render(placement)
        } else {
            displayAd(placement)
        }
    }

    override fun showAd(activity: Activity) {
        showAd(activity, "default")
    }

    override fun hideAd(activity: Activity) {
        logInfo(tag, "Hide ad")
        displaying.value = false
        adRenderer.hide(activity)
        displayingJob?.cancel()
        displayingJob = null
    }

    override fun destroyAd(activity: Activity) {
        logInfo(tag, "Destroy ad")
        hideAd(activity)
        currentBannerView?.destroyAd()
        currentBannerView = null
    }

    override fun setBannerListener(listener: BannerListener?) {
        publisherListener = listener
    }

    override fun notifyLoss(activity: Activity, winnerDemandId: String, winnerEcpm: Double) {}
    override fun notifyWin() {}

    private fun displayAd(placement: String) {
        displayingJob?.cancel()
        displayingJob = scope.launch {
            val next = adCache.poll()
            val activity = weakActivity.get() ?: return@launch
            displaying.first { it }
            pauseResumeObserver.lifecycleFlow.first { it == ActivityLifecycleState.Resumed }
            currentBannerView = getBannerView(activity, next)
            render(placement)
        }
    }

    private fun render(placement: String) {
        val activity = getActivity ?: return
        val bannerView = currentBannerView
        if (bannerView == null) {
            logInfo(tag, "No loaded ad")
            showAfterLoad.set(true)
            publisherListener?.onAdShowFailed(BidonError.AdNotReady)
            return
        }
        showAfterLoad.set(false)
        bannerView.setBannerListener(listener)
        if (!bannerView.isReady()) {
            logInfo(tag, "Source network banner is not ready ${bannerView.children.firstOrNull()}")
        }

        /**
         * RenderAd
         */
        logInfo(tag, "RenderAd $bannerView at $activity")
        adRenderer.render(
            activity = activity,
            bannerView = bannerView,
            positionState = positionState,
            animate = true,
            handleConfigurationChanges = false,
            renderListener = object : AdRenderer.RenderListener {
                override fun onRendered() {
                    logInfo(tag, "RenderListener.onRendered")
                    isDisplaying = true
                }

                override fun onRenderFailed() {
                    logInfo(tag, "RenderListener.onRenderFailed")
                }

                override fun onVisibilityIssued() {
                    bannerView.destroyAd()
                    publisherListener?.onAdShowFailed(BidonError.AdNotReady)
                    logInfo(tag, "RenderListener.onVisibilityIssued")
                }
            }
        )
        startAutoRefresh(placement)
    }

    private fun startAutoRefresh(placement: String) {
        if (refreshTimeout > 0L) {
            logInfo(tag, "Refresh started with timeout $refreshTimeout ms")
            refresher.startRefresh(placement, refreshTimeout) {
                logInfo(tag, "Refresh timed out. Current timeout is $refreshTimeout ms")
                if (refreshTimeout > 0L) {
                    displayAd(placement)
                }
            }
        }
    }

    private fun getBannerView(
        activity: Activity,
        winner: AuctionResult
    ) = BannerView2(
        context = activity.applicationContext,
        auctionResult = winner,
        demandAd = demandAd
    ).apply {
        setBannerFormat(bannerFormat)
    }

    override fun withSettings(settings: Cacheable.Settings) {
        adCache.withSettings(settings)
    }
}