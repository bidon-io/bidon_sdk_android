package org.bidon.sdk.adapter

/**
 * Created by Bidon Team on 06/02/2023.
 */
sealed interface AdProvider {
    interface Interstitial<T : AdAuctionParams> : AdProvider {
        fun interstitial(): AdSource.Interstitial<T>
    }

    interface Banner<T : AdAuctionParams> : AdProvider {
        fun banner(): AdSource.Banner<T>
    }

    interface Rewarded<T : AdAuctionParams> : AdProvider {
        fun rewarded(): AdSource.Rewarded<T>
    }
}