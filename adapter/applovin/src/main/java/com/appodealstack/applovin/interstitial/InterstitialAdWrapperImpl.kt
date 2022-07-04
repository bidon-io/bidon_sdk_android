package com.appodealstack.applovin.interstitial

import android.app.Activity
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import com.applovin.mediation.ads.MaxInterstitialAd
import com.appodealstack.applovin.*
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.base.AdType
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.AdRevenueListener
import com.appodealstack.mads.demands.DemandAd

internal class InterstitialAdWrapperImpl(
    private val adUnitId: String,
    private val activity: Activity
) : InterstitialAdWrapper {

    private val maxInterstitialAd: MaxInterstitialAd by lazy {
        MaxInterstitialAd(adUnitId, activity)
    }
    override val demandAd: DemandAd by lazy {
        DemandAd(
            demandId = ApplovinMaxDemandId,
            adType = AdType.Interstitial,
            objRequest = maxInterstitialAd
        )
    }

    override val isReady: Boolean
        get() = SdkCore.canShow(demandAd)

    override fun loadAd() {
        SdkCore.loadAd(demandAd)
    }

    override fun getAdUnitId(): String = adUnitId

    override fun showAd(
        placement: String?,
        customData: String?,
        containerView: ViewGroup?,
        lifecycle: Lifecycle?
    ) {
        SdkCore.showAd(
            demandAd = demandAd,
            adParams = bundleOf(adUnitIdKey to adUnitId, placementKey to placement, customDataKey to customData),
            showItself = {
                maxInterstitialAd.showAd(placement, customData, containerView, lifecycle)
            }
        )
    }

    override fun setListener(adListener: AdListener) {
        SdkCore.setListener(demandAd, adListener)
    }

    override fun setRevenueListener(adRevenueListener: AdRevenueListener) {
        SdkCore.setRevenueListener(demandAd, adRevenueListener)

    }

    override fun destroy() {
        SdkCore.destroyAd(demandAd, bundleOf(adUnitIdKey to adUnitId))
    }

    override fun setExtraParameter(key: String, value: String?) {
        SdkCore.setExtras(
            demandAd,
            bundleOf(
                keyKey to key,
                valueKey to value,
                adUnitIdKey to adUnitId
            )
        )
    }
}