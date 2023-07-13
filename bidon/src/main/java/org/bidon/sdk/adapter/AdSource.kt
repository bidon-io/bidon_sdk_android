package org.bidon.sdk.adapter

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.stats.StatisticsCollector

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
sealed interface AdSource<T : AdAuctionParams> : StatisticsCollector {
    val demandId: DemandId
    val ad: Ad?
    val adEvent: Flow<AdEvent>
    val isAdReadyToShow: Boolean

    fun destroy()
    fun obtainAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams>

    interface Interstitial<T : AdAuctionParams> : AdSource<T> {
        /**
         * DTExchange, Applovin, UnityAds need [Activity] instance for displaying ad 🤦‍️
         */
        fun show(activity: Activity)
    }
    interface Rewarded<T : AdAuctionParams> : AdSource<T> {
        fun show(activity: Activity)
    }
    interface Banner<T : AdAuctionParams> : AdSource<T> {
        fun getAdView(): AdViewHolder?
    }
}
