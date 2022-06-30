package com.appodealstack.mads.demands

interface AdListener: ExtendedListener {

    /**
     * Callback invokes after auction completed and winner is selected.
     * Calls immediately after [ExtendedListener.onWinnerSelected]
     */
    fun onAdLoaded(ad: BidOnAd?)

    /**
     * Callback invokes after auction completed, but no winner found.
     */
    fun onAdLoadFailed(adUnitId: String?, error: DemandError?)
    fun onAdDisplayed(ad: BidOnAd?)
    fun onAdDisplayFailed(ad: BidOnAd?, error: DemandError?)
    fun onAdClicked(ad: BidOnAd?)
    fun onAdHidden(ad: BidOnAd?)
}

interface AdRevenueListener {
    fun onAdRevenuePaid(ad: BidOnAd?)
}

interface ExtendedListener {
    fun onDemandAdLoaded(demandId: DemandId, ad: BidOnAd) {}
    fun onDemandAdLoadFailed(demandId: DemandId, error: DemandError?) {}

    /**
     * Callback invokes after auction completed and winner is selected.
     * Calls immediately before [AdListener.onAdLoaded]
     */
    fun onWinnerSelected(ad: BidOnAd) {}
}