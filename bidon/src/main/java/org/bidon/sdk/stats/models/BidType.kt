package org.bidon.sdk.stats.models

/**
 * Created by Aleksei Cherniaev on 11/09/2023.
 */
public enum class BidType(public val code: String) {
    /**
     * Real time bidding
     */
    RTB("RTB"),

    /**
     * Pseudo-bidding via eCPM
     */
    CPM("CPM"),
}