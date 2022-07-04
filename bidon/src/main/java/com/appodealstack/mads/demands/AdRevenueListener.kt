package com.appodealstack.mads.demands

import com.appodealstack.mads.auctions.AuctionData

interface AdRevenueListener {
    fun onAdRevenuePaid(ad: AuctionData.Success)
}