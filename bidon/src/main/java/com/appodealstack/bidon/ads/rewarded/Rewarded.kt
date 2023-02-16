package com.appodealstack.bidon.ads.rewarded

import android.app.Activity
import com.appodealstack.bidon.BidOnSdk.Companion.DefaultMinPrice
import com.appodealstack.bidon.BidOnSdk.Companion.DefaultPlacement

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class Rewarded(
    override val placementId: String = DefaultPlacement
) : RewardedAd by RewardedImpl(placementId)

interface RewardedAd {
    val placementId: String

    fun load(activity: Activity, minPrice: Double = DefaultMinPrice)
    fun destroy()
    fun show(activity: Activity)
    fun setRewardedListener(listener: RewardedListener)
}
