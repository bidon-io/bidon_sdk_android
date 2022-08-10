package com.appodealstack.bidon.adapters

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
    fun getUserAdRevenueListener(demandAd: DemandAd): AdRevenueListener?
    fun setAdRevenueListener(demandAd: DemandAd, adRevenueListener: AdRevenueListener)
}

class AdRevenueSourceImpl : AdRevenueSource {
    private val adRevenueListeners = mutableMapOf<DemandAd, AdRevenueListener>()

    override fun getUserAdRevenueListener(demandAd: DemandAd): AdRevenueListener? {
        return adRevenueListeners[demandAd]
    }

    override fun setAdRevenueListener(demandAd: DemandAd, adRevenueListener: AdRevenueListener) {
        adRevenueListeners[demandAd] = adRevenueListener
    }
}