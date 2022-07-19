package com.appodealstack.mads.core

import com.appodealstack.mads.auctions.AuctionListener
import com.appodealstack.mads.demands.AdListener
import com.appodealstack.mads.demands.DemandAd

internal interface ListenersHolder {
    val auctionListener: AuctionListener

    fun addUserListener(demandAd: DemandAd, adListener: AdListener?)
    fun getListenerForDemand(demandAd: DemandAd): AdListener
}