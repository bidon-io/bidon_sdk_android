package com.applovin.mediation.adapters.keeper

import androidx.annotation.VisibleForTesting
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.adapters.banner.BannerAdInstance
import com.applovin.mediation.adapters.interstitial.InterstitialAdInstance
import com.applovin.mediation.adapters.rewarded.RewardedAdInstance
import java.util.concurrent.ConcurrentHashMap

internal object AdKeepers {

    @VisibleForTesting
    internal val keepers = ConcurrentHashMap<String, AdKeeper<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T> getKeeper(maxAdUnitId: String, format: MaxAdFormat): AdKeeper<T> {
        return keepers.getOrPut(maxAdUnitId) {
            when (format) {
                MaxAdFormat.BANNER -> AdKeeperImpl<BannerAdInstance>("Banner_$maxAdUnitId")
                MaxAdFormat.MREC -> AdKeeperImpl<BannerAdInstance>("MRec_$maxAdUnitId")
                MaxAdFormat.LEADER -> AdKeeperImpl<BannerAdInstance>("Leader_$maxAdUnitId")
                MaxAdFormat.INTERSTITIAL -> AdKeeperImpl<InterstitialAdInstance>("Interstitial_$maxAdUnitId")
                MaxAdFormat.REWARDED -> AdKeeperImpl<RewardedAdInstance>("Rewarded_$maxAdUnitId")
                else -> throw IllegalArgumentException("Unsupported ad format: $format")
            }
        } as AdKeeper<T>
    }
}
