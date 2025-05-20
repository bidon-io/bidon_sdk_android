package com.applovin.mediation.adapters.keeper

import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.adapters.banner.BannerAdInstance
import com.applovin.mediation.adapters.interstitial.InterstitialAdInstance
import com.applovin.mediation.adapters.rewarded.RewardedAdInstance

internal object AdKeepers {
    val interstitial: AdKeeper<InterstitialAdInstance> by lazy {
        AdKeeperImpl<InterstitialAdInstance>("Interstitial")
    }

    val rewarded: AdKeeper<RewardedAdInstance> by lazy {
        AdKeeperImpl<RewardedAdInstance>("Rewarded")
    }

    private val banner: AdKeeper<BannerAdInstance> by lazy {
        AdKeeperImpl<BannerAdInstance>("Banner")
    }

    private val mrec: AdKeeper<BannerAdInstance> by lazy {
        AdKeeperImpl<BannerAdInstance>("MRec")
    }

    private val leader: AdKeeper<BannerAdInstance> by lazy {
        AdKeeperImpl<BannerAdInstance>("Leader")
    }

    fun getBannerKeeper(format: MaxAdFormat): AdKeeper<BannerAdInstance> = when (format) {
        MaxAdFormat.BANNER -> banner
        MaxAdFormat.MREC -> mrec
        MaxAdFormat.LEADER -> leader
        else -> banner
    }
}
