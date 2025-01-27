package org.bidon.sdk.ads.banner

import android.app.Activity
import android.graphics.Point
import android.graphics.PointF
import androidx.core.view.children
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AuctionInfo
import org.bidon.sdk.ads.banner.refresh.BannersCache
import org.bidon.sdk.ads.banner.refresh.BannersCacheImpl
import org.bidon.sdk.ads.banner.render.AdRenderer
import org.bidon.sdk.ads.banner.render.AdRenderer.PositionState
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.databinders.extras.ExtrasImpl
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.ext.TAG
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Aleksei Cherniaev on 05/09/2023.
 */
class BannerManager private constructor(
    private val bannersCache: BannersCache,
    private val extras: Extras,
    private val auctionKey: String? = null,
) : PositionedBanner,
    Extras {

    @JvmOverloads
    constructor(auctionKey: String? = null) : this(
        bannersCache = BannersCacheImpl(),
        extras = ExtrasImpl(),
        auctionKey = auctionKey,
    ) {
        logInfo(tag, "Created $this")
    }

    private val tag get() = TAG
    private var weakActivity = WeakReference<Activity>(null)
    private var nextBannerView: BannerView? = null
    private var nextAd: Ad? = null
    private var nextAuctionInfo: AuctionInfo? = null

    private var currentBannerView: BannerView? = null
    private val showAfterLoad = AtomicBoolean(false)
    private var positionState: PositionState = PositionState.Default
    private var publisherListener: BannerListener? = null
    private val adRenderer: AdRenderer by lazy { get() }
    private var _bannerFormat: BannerFormat = BannerFormat.Banner

    override val bannerFormat: BannerFormat get() = _bannerFormat
    override val adSize: AdSize? get() = currentBannerView?.adSize

    override var isDisplaying: Boolean = false
        private set

    /**
     * Positioning functions
     */
    override fun setPosition(position: BannerPosition) {
        logInfo(tag, "Set position $position")
        positionState = PositionState.Place(position)
        if (!BidonSdk.isInitialized()) {
            logInfo(TAG, "Sdk is not initialized")
            return
        }
        if (isDisplaying) {
            weakActivity.get()?.let { activity ->
                showAd(activity)
            }
        }
    }

    override fun setCustomPosition(offset: Point, rotation: Int, anchor: PointF) {
        logInfo(tag, "Set position by coordinates Offset($offset), Rotation($rotation), Anchor($anchor)")
        positionState = PositionState.Coordinate(
            AdRenderer.AdContainerParams(offset, rotation, anchor)
        )
        if (!BidonSdk.isInitialized()) {
            logInfo(TAG, "Sdk is not initialized")
            return
        }
        if (isDisplaying) {
            weakActivity.get()?.let { activity ->
                showAd(activity)
            }
        }
    }

    /**
     * BannerView's functions
     */
    override fun setBannerFormat(bannerFormat: BannerFormat) {
        _bannerFormat = bannerFormat
    }

    override fun loadAd(activity: Activity, pricefloor: Double) {
        activity.runOnUiThread {
            weakActivity = WeakReference(activity)
            if (!BidonSdk.isInitialized()) {
                publisherListener?.onAdLoadFailed(null, BidonError.SdkNotInitialized)
                return@runOnUiThread
            }
            val nextBannerView = nextBannerView
            if (nextBannerView != null) {
                logInfo(tag, "Ad is already loaded")
                nextAd?.let {
                    publisherListener?.onAdLoaded(
                        ad = it,
                        auctionInfo = requireNotNull(nextAuctionInfo) {
                            "Could not receive nextAuctionInfo"
                        }
                    )
                }
                return@runOnUiThread
            }
            bannersCache.get(
                activity = activity,
                format = bannerFormat,
                pricefloor = pricefloor,
                auctionKey = auctionKey,
                extras = extras,
                onLoaded = { ad, auctionInfo, bannerView ->
                    this.nextBannerView = bannerView
                    this.nextAd = ad
                    nextAuctionInfo = auctionInfo
                    publisherListener?.onAdLoaded(ad, auctionInfo)
                    if (showAfterLoad.getAndSet(false) || isDisplaying) {
                        weakActivity.get()?.let { activity ->
                            showAd(activity)
                        }
                    }
                },
                onFailed = { auctionInfo, cause ->
                    publisherListener?.onAdLoadFailed(auctionInfo, cause)
                }
            )
        }
    }

    override fun isReady(): Boolean = nextBannerView?.isReady() == true

    override fun showAd(activity: Activity) {
        logInfo(tag, "Show ad. ${Thread.currentThread()}")
        activity.runOnUiThread {
            weakActivity = WeakReference(activity)
            if (!BidonSdk.isInitialized()) {
                publisherListener?.onAdLoadFailed(null, BidonError.SdkNotInitialized)
                return@runOnUiThread
            }
            val bannerView = nextBannerView ?: currentBannerView
            if (bannerView == null) {
                logInfo(tag, "No loaded ad")
                showAfterLoad.set(true)
                publisherListener?.onAdShowFailed(BidonError.AdNotReady)
                return@runOnUiThread
            }
            if (!bannerView.isReady()) {
                logInfo(tag, "Source network banner is not ready ${bannerView.children.firstOrNull()}")
            }
            nextBannerView = null
            nextAd = null
            currentBannerView = bannerView

            /**
             * RenderAd
             */
            logInfo(tag, "RenderAd at $activity")
            bannerView.setBannerListener(
                object : BannerListener {
                    override fun onAdLoaded(ad: Ad, auctionInfo: AuctionInfo) {}
                    override fun onAdLoadFailed(auctionInfo: AuctionInfo?, cause: BidonError) {}

                    override fun onAdShown(ad: Ad) {
                        activity.runOnUiThread {
                            publisherListener?.onAdShown(ad)
                        }
                    }

                    override fun onAdClicked(ad: Ad) {
                        activity.runOnUiThread {
                            publisherListener?.onAdClicked(ad)
                        }
                    }

                    override fun onAdExpired(ad: Ad) {
                        activity.runOnUiThread {
                            publisherListener?.onAdExpired(ad)
                        }
                    }

                    override fun onRevenuePaid(ad: Ad, adValue: AdValue) {
                        activity.runOnUiThread {
                            publisherListener?.onRevenuePaid(ad, adValue)
                        }
                    }

                    override fun onAdShowFailed(cause: BidonError) {
                        activity.runOnUiThread {
                            publisherListener?.onAdShowFailed(cause)
                        }
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
                        isDisplaying = true
                    }

                    override fun onRenderFailed() {
                        logInfo(tag, "RenderListener.onRenderFailed")
                    }

                    override fun onVisibilityIssued() {
                        activity.runOnUiThread {
                            bannerView.destroyAd()
                            publisherListener?.onAdShowFailed(BidonError.AdNotReady)
                            logInfo(tag, "RenderListener.onVisibilityIssued")
                        }
                    }
                }
            )
        }
    }

    override fun hideAd(activity: Activity) {
        logInfo(tag, "Hide ad.")
        if (!BidonSdk.isInitialized()) {
            logInfo(TAG, "Sdk is not initialized")
            return
        }
        activity.runOnUiThread {
            isDisplaying = false
            showAfterLoad.set(false)
            adRenderer.hide(activity)
        }
    }

    override fun destroyAd(activity: Activity) {
        if (!BidonSdk.isInitialized()) {
            logInfo(TAG, "Sdk is not initialized")
            return
        }
        logInfo(tag, "Destroy ad.")
        activity.runOnUiThread {
            isDisplaying = false
            showAfterLoad.set(false)
            adRenderer.destroy(activity)
            currentBannerView?.destroyAd()
            currentBannerView = null
            nextBannerView?.destroyAd()
            nextBannerView = null
            nextAd = null
            bannersCache.clear()
        }
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

    override fun notifyLoss(activity: Activity, winnerDemandId: String, winnerPrice: Double) {
        activity.runOnUiThread {
            nextBannerView?.notifyLoss(winnerDemandId, winnerPrice)
            nextBannerView = null
            nextAd = null
        }
    }

    override fun notifyWin() {
        nextBannerView?.notifyWin()
    }
}
