package com.appodealstack.bidon.analytics.domain

import com.appodealstack.bidon.adapters.DemandId
import com.appodealstack.bidon.analytics.BidStatsProvider
import com.appodealstack.bidon.analytics.data.models.RoundStatus
import com.appodealstack.bidon.auctions.data.models.BidStat
import com.appodealstack.bidon.core.ext.SystemTimeNow

class BidStatsProviderImpl(auctionId: String, roundId: String, demandId: DemandId) : BidStatsProvider {

    private var stat: BidStat = BidStat(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId,
        startTs = null,
        adUnitId = null,
        finishTs = null,
        roundStatus = null,
        ecpm = null
    )

    override fun onBidStarted(adUnitId: String?) {
        stat = stat.copy(
            startTs = SystemTimeNow,
            adUnitId = adUnitId
        )
    }

    override fun onBidFinished(roundStatus: RoundStatus, ecpm: Double?) {
        stat = stat.copy(
            finishTs = SystemTimeNow,
            roundStatus = roundStatus,
            ecpm = ecpm,
        )
    }

    override fun onWin() {
        stat = stat.copy(
            roundStatus = RoundStatus.Win
        )
    }

    override fun onLoss() {
        stat = stat.copy(
            roundStatus = RoundStatus.Loss
        )
    }

    override fun buildBidStatistic(): BidStat = stat
}