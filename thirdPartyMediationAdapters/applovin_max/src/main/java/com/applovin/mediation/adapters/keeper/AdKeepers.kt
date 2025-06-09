package com.applovin.mediation.adapters.keeper

import androidx.annotation.VisibleForTesting
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.adapters.banner.BannerAdInstance
import com.applovin.mediation.adapters.interstitial.InterstitialAdInstance
import com.applovin.mediation.adapters.rewarded.RewardedAdInstance
import java.util.concurrent.ConcurrentHashMap

/**
 * Centralized registry that manages [AdKeeper] instances for different ad formats and units.
 * Supports configurable behavior (e.g. shared keeper per format) via [Settings].
 */
internal object AdKeepers {

    @VisibleForTesting
    internal val keepers = ConcurrentHashMap<String, AdKeeper<*>>()

    private var settings: Settings = Settings()

    /**
     * Returns an existing keeper or creates a new one.
     * If [Settings.singleKeeperEnabled] is true, the keeper is shared per [MaxAdFormat],
     * otherwise each ad unit ID gets its own instance.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getKeeper(maxAdUnitId: String, format: MaxAdFormat): AdKeeper<T> {
        val key = if (settings.singleKeeperEnabled) format.label else "${format.label}_$maxAdUnitId"
        return keepers.getOrPut(key) {
            when (format) {
                MaxAdFormat.BANNER, MaxAdFormat.MREC, MaxAdFormat.LEADER -> AdKeeperImpl<BannerAdInstance>(key)
                MaxAdFormat.INTERSTITIAL -> AdKeeperImpl<InterstitialAdInstance>(key)
                MaxAdFormat.REWARDED -> AdKeeperImpl<RewardedAdInstance>(key)
                else -> throw IllegalArgumentException("Unsupported ad format: $format")
            }
        } as AdKeeper<T>
    }

    /**
     * Applies runtime settings that control keeper allocation strategy.
     * Should be called once at adapter init.
     */
    fun withSettings(settings: Settings) {
        this.settings = settings
    }

    /**
     * Configuration model for keeper behavior.
     * Can be extended with more flags (e.g. LRU eviction, max capacity, logging).
     *
     * @property singleKeeperEnabled If true, uses one keeper per format instead of per ad unit ID.
     */
    class Settings(
        val singleKeeperEnabled: Boolean = false
    ) {
        companion object {
            /**
             * Parses [Settings] from a map of initialization parameters.
             * Recognizes: "single_keeper_enabled" → true|false.
             */
            fun from(parameters: Map<String, String>): Settings {
                val enabled = parameters["single_keeper_enabled"]?.toBooleanStrictOrNull() == true
                return Settings(singleKeeperEnabled = enabled)
            }
        }
    }
}
