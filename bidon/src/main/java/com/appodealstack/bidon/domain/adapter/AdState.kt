package com.appodealstack.bidon.domain.adapter

import com.appodealstack.bidon.domain.auction.AuctionResult
import com.appodealstack.bidon.domain.common.Ad
import com.appodealstack.bidon.domain.common.Reward
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
sealed interface AdState {
    class Expired(val ad: Ad) : AdState
    class Bid(val result: AuctionResult) : AdState
    class LoadFailed(val cause: Throwable) : AdState
    class Fill(val ad: Ad) : AdState
    class Clicked(val ad: Ad) : AdState
    class Closed(val ad: Ad) : AdState
    class Impression(val ad: Ad) : AdState
    class OnReward(val ad: Ad, val reward: Reward?) : AdState
    class ShowFailed(val cause: Throwable) : AdState
}