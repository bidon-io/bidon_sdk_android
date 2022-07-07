package com.appodealstack.applovin.interstitial

import android.app.Activity
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import com.appodealstack.applovin.*
import com.appodealstack.mads.SdkCore
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.AdRevenueListener
import com.appodealstack.mads.demands.AdType
import com.appodealstack.mads.demands.DemandAd
import java.lang.ref.WeakReference

internal class InterstitialAdWrapperImpl(
    private val adUnitId: String,
    activity: Activity
) : InterstitialAdWrapper {

    private val activityRef = WeakReference(activity)

    override val demandAd: DemandAd by lazy {
        DemandAd(
            adType = AdType.Interstitial,
        )
    }

    override val isReady: Boolean
        get() = SdkCore.canShow(demandAd)

    override fun loadAd() {
        SdkCore.loadAd(activityRef.get(), demandAd, bundleOf(adUnitIdKey to adUnitId))
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
            activity = activityRef.get(),
            adParams = bundleOf(placementKey to placement, customDataKey to customData),
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