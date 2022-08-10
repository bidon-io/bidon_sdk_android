package com.appodealstack.bidon.core

import com.appodealstack.bidon.auctions.AuctionListener
import com.appodealstack.bidon.adapters.AdListener
import com.appodealstack.bidon.adapters.DemandAd

internal interface ListenersHolder {
    val auctionListener: AuctionListener

    fun addUserListener(demandAd: DemandAd, adListener: AdListener?)
    fun getListenerForDemand(demandAd: DemandAd): AdListener
}