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

    override fun onBidStarted(adUnitId: String?): BidStatsProvider {
        stat = stat.copy(
            startTs = SystemTimeNow,
            adUnitId = adUnitId
        )
        return this
    }

    override fun onBidFinished(roundStatus: RoundStatus, ecpm: Double?): BidStatsProvider {
        stat = stat.copy(
            finishTs = SystemTimeNow,
            roundStatus = roundStatus,
            ecpm = ecpm,
        )
        return this
    }

    override fun buildBidStatistic(): BidStat = stat
}