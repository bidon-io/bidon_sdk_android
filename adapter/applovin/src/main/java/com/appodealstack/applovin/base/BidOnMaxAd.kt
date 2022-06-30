package com.appodealstack.applovin.base

import com.applovin.mediation.MaxAd
import com.appodealstack.mads.demands.BidOnAd
import com.appodealstack.mads.demands.DemandId

class BidOnMaxAd(override val sourceAd: MaxAd) : BidOnAd {
    override val demandId: DemandId get() = DemandId(sourceAd.networkName)
    override val ecpm: Double get() = sourceAd.revenue
}

internal fun MaxAd.asBidOnAd() = BidOnMaxAd(this)
