package com.appodealstack.applovin.ext

import android.content.Context
import androidx.core.os.bundleOf
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.ads.MaxAdView
import com.appodealstack.applovin.banner.MaxAdViewWrapper
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.demands.*

internal class MaxAdViewWrapperImpl(
    private val adUnitId: String,
    context: Context
) : MaxAdViewWrapper {

    private val adView by lazy {
        MaxAdView(adUnitId, context)
    }

    private val demandAd = DemandAd(AdType.Banner)

    override fun setListener(listener: BNMaxAdViewAdListener) {
        SdkCore.setListener(demandAd, listener.asAdListener(adUnitId))
    }

    override fun setRevenueListener(listener: AdRevenueListener) {
        TODO("Not yet implemented")
    }

    override fun loadAd() {
        TODO("Not yet implemented")
    }

    override fun setBackgroundColor(color: Int) {
        adView.setBackgroundColor(color)
    }

    override fun setAlpha(alpha: Float) {
        adView.alpha = alpha
    }

    override fun destroy() {
        SdkCore.destroyAd(demandAd, bundleOf())
    }

    override fun getAdUnitId(): String = adUnitId

    override fun getAdFormat(): MaxAdFormat = adView.adFormat

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
        TODO("Not yet implemented")
    }

    override fun getPlacement(): String? = adView.placement
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