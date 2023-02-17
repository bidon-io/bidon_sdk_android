package com.appodealstack.bidon.adapter

import com.appodealstack.bidon.ads.Ad
import com.appodealstack.bidon.ads.rewarded.Reward
import com.appodealstack.bidon.auction.AuctionResult
import com.appodealstack.bidon.config.BidonError

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
sealed interface AdEvent {
    class Expired(val ad: Ad) : AdEvent
    class Bid(val result: AuctionResult) : AdEvent
    class LoadFailed(val cause: BidonError) : AdEvent
    class Fill(val ad: Ad) : AdEvent
    class Clicked(val ad: Ad) : AdEvent
    class Closed(val ad: Ad) : AdEvent
    class Shown(val ad: Ad) : AdEvent
    class PaidRevenue(val ad: Ad) : AdEvent
    class OnReward(val ad: Ad, val reward: Reward?) : AdEvent
    class ShowFailed(val cause: BidonError) : AdEvent
}