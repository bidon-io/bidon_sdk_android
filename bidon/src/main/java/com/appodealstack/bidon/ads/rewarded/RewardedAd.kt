package com.appodealstack.bidon.ads.rewarded

import android.app.Activity
import com.appodealstack.bidon.BidOnSdk.DefaultMinPrice
import com.appodealstack.bidon.BidOnSdk.DefaultPlacement

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class RewardedAd @JvmOverloads constructor(
    override val placementId: String = DefaultPlacement
) : Rewarded by RewardedImpl(placementId)

interface Rewarded {
    val placementId: String

    fun isReady(): Boolean // for show
    fun loadAd(activity: Activity, minPrice: Double = DefaultMinPrice)
    fun destroyAd()
    fun showAd(activity: Activity)
    fun setRewardedListener(listener: RewardedListener)
}
