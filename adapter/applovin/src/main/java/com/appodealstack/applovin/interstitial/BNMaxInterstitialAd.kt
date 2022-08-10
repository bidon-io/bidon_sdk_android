package com.appodealstack.applovin.interstitial

import android.app.Activity
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import com.appodealstack.bidon.adapters.AdRevenueListener
import com.appodealstack.bidon.adapters.DemandAd

/**
 * Full screen Interstitial Advertising
 */
class BNMaxInterstitialAd(
    adUnitId: String,
    activity: Activity
) : InterstitialAdWrapper by InterstitialAdWrapperImpl(adUnitId, activity)

/**
 * [BNMaxInterstitialAd] class description
 */
interface InterstitialAdWrapper {

    val demandAd: DemandAd

    val isReady: Boolean

    fun loadAd()

    fun showAd(
        placement: String? = null,
        customData: String? = null,
        containerView: ViewGroup? = null,
        lifecycle: Lifecycle? = null
    )

    fun getAdUnitId(): String
    fun setListener(bnInterstitialListener: BNInterstitialListener)
    fun setRevenueListener(adRevenueListener: AdRevenueListener)

    fun setExtraParameter(key: String, value: String?)

    fun destroy()
}


