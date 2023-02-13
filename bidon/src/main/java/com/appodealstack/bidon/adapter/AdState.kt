package com.appodealstack.bidon.adapter

import com.appodealstack.bidon.ads.Ad
import com.appodealstack.bidon.ads.BidonError
import com.appodealstack.bidon.ads.rewarded.Reward
import com.appodealstack.bidon.auction.AuctionResult

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
sealed interface AdState {
    class Expired(val ad: Ad) : AdState
    class Bid(val result: AuctionResult) : AdState
    class LoadFailed(val cause: BidonError) : AdState
    class Fill(val ad: Ad) : AdState
    class Clicked(val ad: Ad) : AdState
    class Closed(val ad: Ad) : AdState
    class Impression(val ad: Ad) : AdState
    class PaidRevenue(val ad: Ad) : AdState
    class OnReward(val ad: Ad, val reward: Reward?) : AdState
    class ShowFailed(val cause: BidonError) : AdState
}