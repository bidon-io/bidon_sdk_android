package com.appodealstack.mads.auctions

import com.appodealstack.mads.base.AdUnit

interface Auction {
    fun setPreBidAdUnits(vararg adUnit: AdUnit)
    fun setMediatorAdUnits(vararg adUnit: AdUnit)
    fun setPostBidAdUnits(vararg adUnit: AdUnit)
}