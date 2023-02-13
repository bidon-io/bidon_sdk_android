package com.appodealstack.bidon.ads.rewarded

import com.appodealstack.bidon.ads.Ad
import com.appodealstack.bidon.ads.AdListener
import com.appodealstack.bidon.auction.AuctionListener
import com.appodealstack.bidon.auction.RoundsListener
import com.appodealstack.bidon.logs.analytic.AdRevenueListener

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface RewardedListener :
    AdListener,
    AdRevenueListener,
    AuctionListener,
    RoundsListener,
    RewardedAdListener

interface RewardedAdListener {
    fun onUserRewarded(ad: Ad, reward: Reward?) {}
}

data class Reward(
    val label: String,
    val amount: Int
)
