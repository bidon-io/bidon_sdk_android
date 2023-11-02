package org.bidon.sdk.stats.models

/**
 * Created by Aleksei Cherniaev on 11/09/2023.
 */
enum class BidType(val code: String) {
    /**
     * Real time bidding
     */
    RTB("RTB"),

    /**
     * Pseudo-bidding via eCPM
     */
    CPM("CPM"),
}