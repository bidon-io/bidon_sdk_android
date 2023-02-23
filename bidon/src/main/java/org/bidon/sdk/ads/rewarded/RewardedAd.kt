package org.bidon.sdk.ads.rewarded

import android.app.Activity
import org.bidon.sdk.BidOnSdk.DefaultPlacement
import org.bidon.sdk.BidOnSdk.DefaultPricefloor

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class RewardedAd @JvmOverloads constructor(
    override val placementId: String = DefaultPlacement
) : Rewarded by RewardedImpl(placementId)

interface Rewarded {
    val placementId: String

    fun isReady(): Boolean // for show
    fun loadAd(activity: Activity, pricefloor: Double = DefaultPricefloor)
    fun destroyAd()
    fun showAd(activity: Activity)
    fun setRewardedListener(listener: RewardedListener)
}
