package com.appodealstack.bidon.analytics

import com.appodealstack.bidon.analytics.data.models.RoundStatus
import com.appodealstack.bidon.auctions.data.models.BidStat

interface BidStatsProvider {
    fun onBidStarted(adUnitId: String? = null)
    fun onBidFinished(roundStatus: RoundStatus, ecpm: Double?)
    fun onWin()
    fun onLoss()

    fun buildBidStatistic(): BidStat
}
