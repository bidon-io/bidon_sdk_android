package org.bidon.amazon

/**
 * Created by Aleksei Cherniaev on 27/09/2023.
 */
internal enum class SlotType(val format: String) {
    /**
     * All banner sizes
     */
    BANNER("BANNER"),
    MREC("MREC"),

    REWARDED_AD("REWARDED"),

    /**
     * Interstitial
     */
    VIDEO("VIDEO"),
    INTERSTITIAL("INTERSTITIAL");

    companion object {
        fun getOrNull(format: String): SlotType? = values().firstOrNull { it.format == format }
    }
}