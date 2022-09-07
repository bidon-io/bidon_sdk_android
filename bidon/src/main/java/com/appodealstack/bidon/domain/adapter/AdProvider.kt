package com.appodealstack.bidon.domain.adapter

import com.appodealstack.bidon.domain.common.DemandAd

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