package com.appodealstack.mads.auctions

import com.appodealstack.mads.demands.DemandAd

internal interface AuctionResultsHolder {
    fun clearResults(demandAd: DemandAd)
    fun addResult(demandAd: DemandAd, result: AuctionData.Success)
    fun updateResults(demandAd: DemandAd, results: List<AuctionData.Success>)
    fun getTopResultOrNull(demandAd: DemandAd): AuctionData.Success?
}

internal class AuctionResultsHolderImpl : AuctionResultsHolder {
    private val resultsMap = mutableMapOf<DemandAd, List<AuctionData.Success>>()

    override fun clearResults(demandAd: DemandAd) {
        resultsMap.remove(demandAd)
    }

    override fun addResult(demandAd: DemandAd, result: AuctionData.Success) {
        resultsMap[demandAd] = (resultsMap[demandAd] ?: emptyList()) + result
    }

    override fun updateResults(demandAd: DemandAd, results: List<AuctionData.Success>) {
        resultsMap[demandAd] = results
    }

    override fun getTopResultOrNull(demandAd: DemandAd): AuctionData.Success? {
        return resultsMap[demandAd]?.firstOrNull()
    }
}