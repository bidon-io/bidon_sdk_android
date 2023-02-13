package com.appodealstack.bidon.adapter

import com.appodealstack.bidon.ads.DemandAd
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
sealed interface AdProvider {
    interface Interstitial<T : AdAuctionParams> : AdProvider {
        fun interstitial(demandAd: DemandAd, roundId: String, auctionId: String): AdSource.Interstitial<T>
    }

    interface Banner<T : AdAuctionParams> : AdProvider {
        fun banner(demandAd: DemandAd, roundId: String, auctionId: String): AdSource.Banner<T>
    }

    interface Rewarded<T : AdAuctionParams> : AdProvider {
        fun rewarded(demandAd: DemandAd, roundId: String, auctionId: String): AdSource.Rewarded<T>
    }
}