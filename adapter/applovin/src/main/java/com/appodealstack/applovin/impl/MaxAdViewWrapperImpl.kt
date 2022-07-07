package com.appodealstack.applovin.impl

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import com.applovin.mediation.MaxAdFormat
import com.appodealstack.applovin.adUnitIdKey
import com.appodealstack.applovin.banner.MaxAdViewWrapper
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.demands.*

internal class MaxAdViewWrapperImpl constructor(
    private val adUnitId: String,
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), MaxAdViewWrapper {

    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
            this("", context, attrs, defStyleAttr)

    private val demandAd = DemandAd(AdType.Banner)

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
                this.removeAllViews()
                this.addView(view)
            }
        )
    }

    override fun destroy() {
        SdkCore.destroyAd(demandAd, bundleOf())
    }

    override fun getAdUnitId(): String = adUnitId

    override fun getAdFormat(): MaxAdFormat = MaxAdFormat.BANNER

    override fun setCustomData(value: String) {
        TODO("Not yet implemented")
    }

    override fun setExtraParameter(key: String, value: String) {
        SdkCore.setExtras(demandAd, bundleOf(key to value))
    }

    override fun startAutoRefresh() {
        TODO("Not yet implemented")
    }

    override fun stopAutoRefresh() {
        TODO("Not yet implemented")
    }

    override fun setPlacement(placement: String?) {
        SdkCore.setPlacement(placement)
    }

    override fun getPlacement(): String? = SdkCore.getPlacement()
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