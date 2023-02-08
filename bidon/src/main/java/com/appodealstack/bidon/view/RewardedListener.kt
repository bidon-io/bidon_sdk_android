package com.appodealstack.bidon.view

import com.appodealstack.bidon.domain.auction.AuctionListener
import com.appodealstack.bidon.domain.auction.RoundsListener
import com.appodealstack.bidon.domain.common.Ad
import com.appodealstack.bidon.domain.common.AdListener

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface RewardedListener : AdListener, AuctionListener, RoundsListener, RewardedAdListener

interface RewardedAdListener {
    fun onUserRewarded(ad: Ad, reward: Reward?) {}
}

data class Reward(
    val label: String,
    val amount: Int
)
