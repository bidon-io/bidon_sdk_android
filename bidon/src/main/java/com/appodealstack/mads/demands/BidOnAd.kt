package com.appodealstack.mads.demands

/**
 * Base marker interface for Demand's Ads
 */
interface BidOnAd {
    val demandId: DemandId
    val ecpm: Double
    val sourceAd: Any?

    /**
     * Experimental. Enable show current [BidOnAd]
     */
    fun show() {
        error("Not yet overridden")
    }
}