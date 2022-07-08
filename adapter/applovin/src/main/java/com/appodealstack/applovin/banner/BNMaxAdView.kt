package com.appodealstack.applovin.banner

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.core.view.children
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.ads.MaxAdView
import com.appodealstack.applovin.R
import com.appodealstack.applovin.adUnitIdKey
import com.appodealstack.applovin.impl.BNMaxAdViewAdListener
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.core.ext.logInternal
import com.appodealstack.mads.demands.*

internal interface MaxAdViewWrapper {
    fun setListener(listener: BNMaxAdViewAdListener)
    fun setRevenueListener(listener: AdRevenueListener)
    fun loadAd()
    fun destroy()
    fun getAdUnitId(): String
    fun getAdFormat(): MaxAdFormat
    fun setCustomData(value: String)
    fun setExtraParameter(key: String, value: String)
    fun startAutoRefresh()
    fun stopAutoRefresh()
    fun setPlacement(placement: String?)
    fun getPlacement(): String?
}

class BNMaxAdView constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr), MaxAdViewWrapper {

    private var adUnitId: String = ""

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(adUnitId: String, context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
            this(context, attrs, defStyleAttr) {
        this.adUnitId = adUnitId
    }

    private val demandAd = DemandAd(AdType.Banner)
    private var autoRefresh: Boolean? = null
    private var customData: String? = null

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BNMaxAdView)
        typedArray.getString(R.styleable.BNMaxAdView_adUnitId)?.let {
            this.adUnitId = it
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
        SdkCore.loadAd(
            context = context,
            demandAd = demandAd,
            adParams = bundleOf(adUnitIdKey to adUnitId),
            onViewReady = { view ->
                setAutoRefresh(view, autoRefresh)
                (view as? MaxAdView)?.setCustomData(customData)
                view.layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT,
                )
                this.removeAllViews()
                this.addView(view)
            }
        )
    }

    override fun destroy() {
        this.removeAllViews()
        SdkCore.destroyAd(demandAd, bundleOf())
    }

    override fun getAdUnitId(): String = adUnitId

    override fun getAdFormat(): MaxAdFormat = MaxAdFormat.BANNER

    override fun setCustomData(value: String) {
        customData = value
        (this.children.firstOrNull() as? MaxAdView)?.setCustomData(value)
    }

    override fun setExtraParameter(key: String, value: String) {
        SdkCore.setExtras(demandAd, bundleOf(key to value))
    }

    override fun startAutoRefresh() {
        autoRefresh = true
        setAutoRefresh(this.children.firstOrNull(), autoRefresh)
        SdkCore.setAutoRefresh(demandAd, autoRefresh = true)
    }

    override fun stopAutoRefresh() {
        autoRefresh = false
        setAutoRefresh(this.children.firstOrNull(), autoRefresh)
    }

    override fun setPlacement(placement: String?) {
        SdkCore.setPlacement(placement)
    }

    override fun getPlacement(): String? = SdkCore.getPlacement()

    private fun setAutoRefresh(view: View?, isOn: Boolean?) {
        if (view is MaxAdView) {
            when (isOn) {
                true -> view.startAutoRefresh()
                false -> view.stopAutoRefresh()
                null -> {}
            }
        }
    }
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