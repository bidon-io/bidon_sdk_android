package org.bidon.sdk.adapter

import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.rewarded.Reward
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue

/**
 * Created by Bidon Team on 06/02/2023.
 */
public sealed interface AdEvent {
    public class Expired(public val ad: Ad) : AdEvent
    public class LoadFailed(public val cause: BidonError) : AdEvent
    public class Fill(public val ad: Ad) : AdEvent
    public class Clicked(public val ad: Ad) : AdEvent
    public class Closed(public val ad: Ad) : AdEvent
    public class Shown(public val ad: Ad) : AdEvent
    public class PaidRevenue(public val ad: Ad, public val adValue: AdValue) : AdEvent
    public class OnReward(public val ad: Ad, public val reward: Reward?) : AdEvent
    public class ShowFailed(public val cause: BidonError) : AdEvent
}