package org.bidon.sdk.ads.banner

import android.app.Activity
import android.graphics.Point
import android.graphics.PointF
import androidx.core.view.children
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.ext.ad
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.banner.helper.getWidthDp
import org.bidon.sdk.ads.banner.render.AdRenderer
import org.bidon.sdk.ads.banner.render.AdRenderer.PositionState
import org.bidon.sdk.ads.interstitial.AdCache
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.WinLossNotifier
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.ext.TAG
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Aleksei Cherniaev on 05/09/2023.
 */
class CachedBanner private constructor(
    private val adCache: AdCache,
    private val extras: Extras,
    private val demandAd: DemandAd = DemandAd(AdType.Banner),
) : PositionedBanner,
    WinLossNotifier,
    Extras {

    constructor() : this(
        adCache = get {
            params(DemandAd(AdType.Banner))
        },
        extras = get()
    ) {
        logInfo(tag, "Created $this")
    }

    private val tag get() = TAG
    private var weakActivity = WeakReference<Activity>(null)
    private var nextBannerView: BannerView2? = null
    private var currentBannerView: BannerView2? = null
    private var bannerFormat: BannerFormat = BannerFormat.Banner
    private val showAfterLoad = AtomicBoolean(false)
    private var positionState: PositionState = PositionState.Default
    private var publisherListener: BannerListener? = null
    private val adRenderer: AdRenderer by lazy { get() }

    override val adSize: AdSize?
        get() = currentBannerView?.adSize

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
        this.bannerFormat = bannerFormat
    }

    override fun loadAd(activity: Activity, pricefloor: Double) {
        weakActivity = WeakReference(activity)
        if (!BidonSdk.isInitialized()) {
            publisherListener?.onAdLoadFailed(BidonError.SdkNotInitialized)
            return
        }
        nextBannerView = adCache.poll()?.let { winner ->
            getBannerView(activity, winner)
        }
        val minCachedPricefloor = adCache.peek()?.adSource?.getStats()?.ecpm ?: 0.0
        adCache.cache(
            adTypeParam = AdTypeParam.Banner(
                activity = activity,
                pricefloor = maxOf(pricefloor, minCachedPricefloor),
                bannerFormat = bannerFormat,
                containerWidth = bannerFormat.getWidthDp().toFloat()
            ),
            onSuccess = { auctionResult ->
                publisherListener?.onAdLoaded(auctionResult.adSource.ad!!)
                if (nextBannerView == null) {
                    nextBannerView = adCache.poll()?.let { winner ->
                        getBannerView(activity, winner)
                    }
                }
                if (showAfterLoad.getAndSet(false)) {
                    weakActivity.get()?.let { activity ->
                        showAd(activity)
                    }
                }
            },
            onFailure = { cause ->
                publisherListener?.onAdLoadFailed(cause)
            }
        )
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

    override fun isReady(): Boolean = currentBannerView?.isReady() == true || nextBannerView?.isReady() == true

    override fun showAd(activity: Activity) {
        weakActivity = WeakReference(activity)
        logInfo(tag, "Show ad")
        if (!BidonSdk.isInitialized()) {
            publisherListener?.onAdLoadFailed(BidonError.SdkNotInitialized)
            return
        }
        val bannerView = nextBannerView ?: currentBannerView
        if (bannerView == null) {
            logInfo(tag, "No loaded ad")
            showAfterLoad.set(true)
            publisherListener?.onAdShowFailed(BidonError.BannerAdNotReady)
            return
        }
        if (!bannerView.isReady()) {
            logInfo(tag, "Source network banner is not ready ${bannerView.children.firstOrNull()}")
        }
        nextBannerView = null
        currentBannerView = bannerView

        /**
         * RenderAd
         */
        showAfterLoad.set(false)
        logInfo(tag, "RenderAd at $activity")
        bannerView.setBannerListener(
            object : BannerListener {
                override fun onAdLoaded(ad: Ad) {}
                override fun onAdLoadFailed(cause: BidonError) {}

                override fun onAdShown(ad: Ad) {
                    loadAd(activity, pricefloor = 0.0)
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
        )
        adRenderer.render(
            activity = activity,
            bannerView = bannerView,
            positionState = positionState,
            animate = true,
            handleConfigurationChanges = false,
            renderListener = object : AdRenderer.RenderListener {
                override fun onRendered() {
                    logInfo(tag, "RenderListener.onRendered")
                }

                override fun onRenderFailed() {
                    logInfo(tag, "RenderListener.onRenderFailed")
                }

                override fun onVisibilityIssued() {
                    bannerView.destroyAd()
                    publisherListener?.onAdShowFailed(BidonError.BannerAdNotReady)
                    logInfo(tag, "RenderListener.onVisibilityIssued")
                }
            }
        )
    }

    override fun hideAd() {
        logInfo(tag, "Hide ad")
        adRenderer.hide()
    }

    override fun destroyAd() {
        logInfo(tag, "Destroy ad")
        hideAd()
        currentBannerView?.destroyAd()
        currentBannerView = null
        nextBannerView?.destroyAd()
        nextBannerView = null
    }

    override fun setBannerListener(listener: BannerListener?) {
        publisherListener = listener
    }

    override fun addExtra(key: String, value: Any?) {
        extras.addExtra(key, value)
        nextBannerView?.addExtra(key, value)
        currentBannerView?.addExtra(key, value)
    }

    override fun getExtras(): Map<String, Any> {
        return extras.getExtras()
    }

    override fun notifyLoss(winnerDemandId: String, winnerEcpm: Double) {
        nextBannerView?.notifyLoss(winnerDemandId, winnerEcpm)
        nextBannerView = null
    }

    override fun notifyWin() {
        nextBannerView?.notifyWin()
    }
}
