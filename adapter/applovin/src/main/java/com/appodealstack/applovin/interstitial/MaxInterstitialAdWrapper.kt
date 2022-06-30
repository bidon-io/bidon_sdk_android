package com.appodealstack.applovin.interstitial

import android.app.Activity
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.AdRevenueListener

/**
 * Full screen Interstitial Advertising
 */
class MaxInterstitialAdWrapper(
    adUnitId: String,
    activity: Activity
) : InterstitialAdWrapper by InterstitialAdWrapperImpl(adUnitId, activity)

/**
 * [MaxInterstitialAdWrapper] class description
 */
interface InterstitialAdWrapper {
    val isReady: Boolean

    fun loadAd()

    fun showAd(
        placement: String? = null,
        customData: String? = null,
        containerView: ViewGroup? = null,
        lifecycle: Lifecycle? = null
    )

    fun getAdUnitId(): String
    fun setListener(adListener: AdListener)
    fun setRevenueListener(adRevenueListener: AdRevenueListener)

    fun setExtraParameter(key: String, value: String?)

    fun destroy()
}


