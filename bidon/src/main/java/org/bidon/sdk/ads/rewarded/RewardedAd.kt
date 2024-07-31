package org.bidon.sdk.ads.rewarded

import android.app.Activity
import org.bidon.sdk.BidonSdk.DefaultPricefloor
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.stats.WinLossNotifier

/**
 * Created by Bidon Team on 06/02/2023.
 */
class RewardedAd @JvmOverloads constructor(
    auctionKey: String? = null
) : Rewarded by RewardedImpl(auctionKey = auctionKey)

interface Rewarded : Extras, WinLossNotifier {
    fun isReady(): Boolean // for show
    fun loadAd(activity: Activity, pricefloor: Double = DefaultPricefloor)
    fun destroyAd()
    fun showAd(activity: Activity)
    fun setRewardedListener(listener: RewardedListener)
}
