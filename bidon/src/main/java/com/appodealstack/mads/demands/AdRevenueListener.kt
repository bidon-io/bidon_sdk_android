package com.appodealstack.mads.demands

interface AdRevenueListener {
    fun onAdRevenuePaid(ad: Ad)
}

/**
 * Sets AdRevenueListener to Existing objects (after auction is completed)
 */
interface AdRevenueProvider {
    fun setAdRevenueListener(adRevenueListener: AdRevenueListener)
}

/**
 * Sets AdRevenueListener to New objects (before auction is started)
 */
interface AdRevenueSource {
    fun setAdRevenueListener(demandAd: DemandAd, adRevenueListener: AdRevenueListener)
}
