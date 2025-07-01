package com.ironsource.adapters.custom.bidon.keeper

import com.ironsource.adapters.custom.bidon.interstitial.InterstitialAdInstance
import java.util.concurrent.ConcurrentHashMap

internal object AdKeepers {
    private val interstitialKeepers = ConcurrentHashMap<String, AdKeeper<InterstitialAdInstance>>()

    fun getInterstitialKeeper(adUnitId: String): AdKeeper<InterstitialAdInstance> =
        interstitialKeepers.getOrPut(adUnitId) { AdKeeperImpl("Interstitial") }
}
