package com.appodealstack.ironsource.banner

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import com.appodealstack.ironsource.ISDecorator
import com.appodealstack.ironsource.PlacementKey
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.auctions.AuctionResolver
import com.appodealstack.mads.core.DefaultAutoRefreshTimeoutMs
import com.appodealstack.mads.demands.Ad
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.AdType
import com.appodealstack.mads.demands.DemandAd
import com.appodealstack.mads.demands.banners.AutoRefresh
import com.appodealstack.mads.demands.banners.BannerSize
import com.appodealstack.mads.demands.banners.BannerSizeKey

class BNIronSourceBannerLayout constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr), ISDecorator.Banner.BannerView {
    private val demandAd by lazy { DemandAd(AdType.Banner) }
    private var bannerSize = BannerSize.Banner
    private var userListener: IronSourceBannerListener? = null
    private var userLevelPlayListener: IronSourceLevelPlayBannerListener? = null
    private var autoRefresh: AutoRefresh = AutoRefresh.On(timeoutMs = DefaultAutoRefreshTimeoutMs)

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, bannerSize: BannerSize) : this(context, attrs, 0) {
        this.bannerSize = bannerSize
    }

    init {
        SdkCore.setListener(demandAd, object : AdListener {
            override fun onAdLoaded(ad: Ad) {
                userListener?.onBannerAdLoaded()
                userLevelPlayListener?.onAdLoaded(ad)
            }

            override fun onAdLoadFailed(cause: Throwable) {
                userListener?.onBannerAdLoadFailed(cause)
                userLevelPlayListener?.onAdLoadFailed(cause)
            }

            override fun onAdDisplayed(ad: Ad) {
                userListener?.onBannerAdScreenPresented()
                userLevelPlayListener?.onAdScreenPresented(ad)

            }

            override fun onAdDisplayFailed(cause: Throwable) {}
            override fun onAdImpression(ad: Ad) {}

            override fun onAdClicked(ad: Ad) {
                userListener?.onBannerAdClicked()
                userLevelPlayListener?.onAdClicked(ad)
            }

            override fun onAdHidden(ad: Ad) {
                userListener?.onBannerAdScreenDismissed()
                userLevelPlayListener?.onAdScreenDismissed(ad)
            }

            override fun onDemandAdLoaded(ad: Ad) {
                userListener?.onDemandAdLoaded(ad)
                userLevelPlayListener?.onDemandAdLoaded(ad)
            }

            override fun onDemandAdLoadFailed(cause: Throwable) {
                userListener?.onDemandAdLoadFailed(cause)
                userLevelPlayListener?.onDemandAdLoadFailed(cause)
            }

            override fun onAuctionFinished(ads: List<Ad>) {
                userListener?.onAuctionFinished(ads)
                userLevelPlayListener?.onAuctionFinished(ads)
            }
        })
    }

    override fun setBannerListener(bannerListener: IronSourceBannerListener) {
        this.userListener = bannerListener
    }

    override fun loadAd(placementName: String?) {
        SdkCore.loadAdView(
            context = context as Activity,
            demandAd = demandAd,
            adParams = bundleOf(PlacementKey to placementName, BannerSizeKey to bannerSize.ordinal),
            autoRefresh = autoRefresh,
            adContainer = this,
            onViewReady = { adView ->
                this.removeAllViews()
                this.addView(
                    adView.apply {
                        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                    })
            }
        )
    }

    override fun setLevelPlayBannerListener(bannerListener: IronSourceLevelPlayBannerListener) {
        this.userLevelPlayListener = bannerListener
    }

    override fun removeBannerListener() {
        this.userListener = null
        this.userLevelPlayListener = null
    }

    override fun destroy() {
        SdkCore.destroyAd(demandAd, bundleOf())
        removeBannerListener()
        this.removeAllViews()
    }

    override fun startAutoRefresh() {
        if (autoRefresh == AutoRefresh.Off) {
            autoRefresh = AutoRefresh.On(DefaultAutoRefreshTimeoutMs)
        }
        SdkCore.setAutoRefresh(demandAd, autoRefresh)
    }

    override fun stopAutoRefresh() {
        autoRefresh = AutoRefresh.Off
        SdkCore.setAutoRefresh(demandAd, autoRefresh)
    }

    override fun setAutoRefreshTimeout(timeoutMs: Long) {
        if (timeoutMs < 0) return
        autoRefresh = if (timeoutMs == 0L) AutoRefresh.Off else AutoRefresh.On(timeoutMs)
        SdkCore.setAutoRefresh(demandAd, autoRefresh)
    }

    override fun setAuctionResolver(auctionResolver: AuctionResolver) {
        SdkCore.saveAuctionResolver(demandAd, auctionResolver)
    }
}