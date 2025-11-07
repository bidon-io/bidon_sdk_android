package org.bidon.sdk.adapter

/**
 * Created by Bidon Team on 06/02/2023.
 */
public sealed interface AdProvider {
    public interface Interstitial<T : AdAuctionParams> : AdProvider {
        public fun interstitial(): AdSource.Interstitial<T>
    }

    public interface Banner<T : AdAuctionParams> : AdProvider {
        public fun banner(): AdSource.Banner<T>
    }

    public interface Rewarded<T : AdAuctionParams> : AdProvider {
        public fun rewarded(): AdSource.Rewarded<T>
    }
}