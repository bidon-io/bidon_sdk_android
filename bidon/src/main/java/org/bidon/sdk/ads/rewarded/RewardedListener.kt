package org.bidon.sdk.ads.rewarded

import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AdListener
import org.bidon.sdk.ads.FullscreenAdListener
import org.bidon.sdk.logs.analytic.AdRevenueListener

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface RewardedListener :
    AdListener,
    AdRevenueListener,
    FullscreenAdListener,
    RewardedAdListener

interface RewardedAdListener {
    fun onUserRewarded(ad: Ad, reward: Reward?) {}
}

data class Reward(
    val label: String,
    val amount: Int
)
