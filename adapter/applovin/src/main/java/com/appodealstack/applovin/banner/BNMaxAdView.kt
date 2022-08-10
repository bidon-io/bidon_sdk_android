package com.appodealstack.applovin.banner

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.core.view.children
import com.applovin.mediation.ads.MaxAdView
import com.appodealstack.applovin.AdUnitIdKey
import com.appodealstack.applovin.AdaptiveBannerHeightKey
import com.appodealstack.applovin.R
import com.appodealstack.applovin.impl.BNMaxAdViewAdListener
import com.appodealstack.bidon.SdkCore
import com.appodealstack.bidon.core.DefaultAutoRefreshTimeoutMs
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.adapters.banners.AutoRefresh
import com.appodealstack.bidon.adapters.banners.BannerSize
import com.appodealstack.bidon.adapters.banners.BannerSizeKey

internal interface MaxAdViewWrapper {
    fun setListener(listener: BNMaxAdViewAdListener)
    fun setRevenueListener(listener: AdRevenueListener)
    fun loadAd()
    fun destroy()
    fun getAdUnitId(): String
    fun getAdFormat(): BannerSize
    fun setCustomData(value: String)
    fun setExtraParameter(key: String, value: String)
    fun startAutoRefresh()
    fun stopAutoRefresh()
    fun setAutoRefreshTimeout(timeoutMs: Long)
    fun setPlacement(placement: String?)
    fun getPlacement(): String?
}

class BNMaxAdView constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr), MaxAdViewWrapper {

    private var adUnitId: String = ""
    private var adFormat: BannerSize = BannerSize.Banner
    private val extras = mutableMapOf<String, String>()

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(adUnitId: String, context: Context) : this(context) {
        this.adUnitId = adUnitId
    }

    constructor(adUnitId: String, adFormat: BannerSize, context: Context) : this(context) {
        this.adUnitId = adUnitId
        this.adFormat = adFormat
    }

    private val demandAd = DemandAd(AdType.Banner)
    private var autoRefresh: AutoRefresh = AutoRefresh.On(timeoutMs = DefaultAutoRefreshTimeoutMs)
    private var customData: String? = null

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BNMaxAdView)
        typedArray.getString(R.styleable.BNMaxAdView_adUnitId)?.let {
            this.adUnitId = it
        }
        typedArray.getInt(R.styleable.BNMaxAdView_adFormat, -1).let {
            when (it) {
                1 -> adFormat = BannerSize.Banner
                2 -> adFormat = BannerSize.MRec
                3 -> adFormat = BannerSize.LeaderBoard
                else -> null
            }
        }
        typedArray.recycle()
    }

    override fun setListener(listener: BNMaxAdViewAdListener) {
        SdkCore.setListener(demandAd, listener.asAdListener(adUnitId))
    }

    override fun setRevenueListener(listener: AdRevenueListener) {
        SdkCore.setRevenueListener(demandAd, listener)
    }

    override fun loadAd() {
        this.post {
            val height = layoutParams.height
            SdkCore.loadAdView(
                context = context,
                demandAd = demandAd,
                adParams = bundleOf(
                    AdUnitIdKey to adUnitId,
                    BannerSizeKey to adFormat.ordinal,
                    AdaptiveBannerHeightKey to height
                ),
                autoRefresh = autoRefresh,
                onViewReady = { view ->
                    (view as? MaxAdView)?.setCustomData(customData)
                    view.layoutParams = LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT,
                    )
                    this.removeAllViews()
                    this.addView(view)
                },
            )
        }
    }

    override fun destroy() {
        this.removeAllViews()
        SdkCore.destroyAd(demandAd, bundleOf())
    }

    override fun getAdUnitId(): String = adUnitId

    override fun getAdFormat(): BannerSize = BannerSize.Banner

    override fun setCustomData(value: String) {
        customData = value
        (this.children.firstOrNull() as? MaxAdView)?.setCustomData(value)
    }

    override fun setExtraParameter(key: String, value: String) {
        extras[key] = value
        SdkCore.setExtras(demandAd, bundleOf(key to value))
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

    override fun setPlacement(placement: String?) {
        SdkCore.setPlacement(demandAd, placement)
    }

    override fun getPlacement(): String? = SdkCore.getPlacement(demandAd)
}

private fun BNMaxAdViewAdListener.asAdListener(adUnitId: String): AdListener {
    return object : AdListener {
        override fun onAdLoaded(ad: Ad) {
            this@asAdListener.onAdLoaded(ad)
        }

        override fun onAdLoadFailed(cause: Throwable) {
            this@asAdListener.onAdLoadFailed(adUnitId, cause)
        }

        override fun onAdDisplayed(ad: Ad) {
        }

        override fun onAdDisplayFailed(cause: Throwable) {
            this@asAdListener.onAdDisplayFailed(cause)
        }

        override fun onAdImpression(ad: Ad) {
        }

        override fun onAdClicked(ad: Ad) {
            this@asAdListener.onAdClicked(ad)
        }

        override fun onAdHidden(ad: Ad) {
        }

        override fun onDemandAdLoaded(ad: Ad) {
            this@asAdListener.onDemandAdLoaded(ad)
        }

        override fun onDemandAdLoadFailed(cause: Throwable) {
            this@asAdListener.onDemandAdLoadFailed(cause)
        }

        override fun onAuctionFinished(ads: List<Ad>) {
            this@asAdListener.onAuctionFinished(ads)
        }
    }
}