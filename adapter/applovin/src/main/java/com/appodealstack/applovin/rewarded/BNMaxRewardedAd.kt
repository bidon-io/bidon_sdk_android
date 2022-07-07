package com.appodealstack.applovin.rewarded

import android.app.Activity
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import com.appodealstack.mads.demands.AdRevenueListener
import com.appodealstack.mads.demands.DemandAd

/**
 * Full screen Interstitial Advertising
 */
class BNMaxRewardedAd(
    adUnitId: String,
    activity: Activity
) : RewardedAdWrapper by RewardedAdWrapperImpl(adUnitId, activity)

/**
 * [BNMaxRewardedAd] class description
 */
interface RewardedAdWrapper {

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
    fun setListener(bnRewardedListener: BNRewardedListener)
    fun setRevenueListener(adRevenueListener: AdRevenueListener)

    fun setExtraParameter(key: String, value: String?)

    fun destroy()
}


