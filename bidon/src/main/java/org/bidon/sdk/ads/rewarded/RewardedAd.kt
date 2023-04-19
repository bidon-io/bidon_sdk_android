package org.bidon.sdk.ads.rewarded

import android.app.Activity
import org.bidon.sdk.BidonSdk.DefaultPricefloor
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.stats.LossNotifier

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class RewardedAd : Rewarded by RewardedImpl()

interface Rewarded : Extras, LossNotifier {
    fun isReady(): Boolean // for show
    fun loadAd(activity: Activity, pricefloor: Double = DefaultPricefloor)
    fun destroyAd()
    fun showAd(activity: Activity)
    fun setRewardedListener(listener: RewardedListener)
}
