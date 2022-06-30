package com.appodealstack.mads.base

import com.appodealstack.mads.demands.DemandId

open class AdUnit(
    val demandId: DemandId,
    val id: String,
    val ecpm: Double?,
)