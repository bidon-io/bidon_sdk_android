package org.bidon.sdk.adapter

import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.rewarded.Reward
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue

/**
 * Created by Bidon Team on 06/02/2023.
 */
sealed interface AdEvent {
    class Expired(val ad: Ad) : AdEvent
    class LoadFailed(val cause: BidonError) : AdEvent
    class Fill(val ad: Ad) : AdEvent
    class Clicked(val ad: Ad) : AdEvent
    class Closed(val ad: Ad) : AdEvent
    class Shown(val ad: Ad) : AdEvent
    class PaidRevenue(val ad: Ad, val adValue: AdValue) : AdEvent
    class OnReward(val ad: Ad, val reward: Reward?) : AdEvent
    class ShowFailed(val cause: BidonError) : AdEvent
}