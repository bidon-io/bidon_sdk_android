package com.appodealstack.applovin.interstitial

import android.app.Activity
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import com.appodealstack.applovin.adUnitIdKey
import com.appodealstack.applovin.customDataKey
import com.appodealstack.applovin.keyKey
import com.appodealstack.applovin.placementKey
import com.appodealstack.applovin.valueKey
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.base.AdType
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.AdRevenueListener

internal class InterstitialAdWrapperImpl(
    private val adUnitId: String,
    private val activity: Activity
) : InterstitialAdWrapper {
    override val isReady: Boolean
        get() = SdkCore.canShow(
            adType = AdType.Interstitial,
            adParams = bundleOf(adUnitIdKey to adUnitId)
        )

    override fun loadAd() {
        SdkCore.loadAd(
            activity = activity,
            adType = AdType.Interstitial,
            adParams = bundleOf(adUnitIdKey to adUnitId)
        )
    }

    override fun getAdUnitId(): String = adUnitId

    override fun showAd(
        placement: String?,
        customData: String?,
        containerView: ViewGroup?,
        lifecycle: Lifecycle?
    ) {
        SdkCore.showAd(
            adType = AdType.Interstitial,
            bundle = bundleOf(adUnitIdKey to adUnitId, placementKey to placement, customDataKey to customData)
        )
    }

    override fun setListener(adListener: AdListener) {
        SdkCore.setListener(AdType.Interstitial, adListener)
    }

    override fun setRevenueListener(adRevenueListener: AdRevenueListener) {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        SdkCore.destroyAd(AdType.Interstitial, bundleOf(adUnitIdKey to adUnitId))
    }

    override fun setExtraParameter(key: String, value: String?) {
        SdkCore.setExtras(
            bundleOf(
                keyKey to key,
                valueKey to value,
                adUnitIdKey to adUnitId
            )
        )
    }
}