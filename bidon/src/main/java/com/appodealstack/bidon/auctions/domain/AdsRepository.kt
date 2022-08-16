package com.appodealstack.bidon.auctions.domain

import com.appodealstack.bidon.adapters.DemandAd
import com.appodealstack.bidon.auctions.Auction
import com.appodealstack.bidon.auctions.data.models.OldAuctionResult

internal interface AdsRepository {
    fun destroyResults(demandAd: DemandAd)
    fun getWinnerOrNull(demandAd: DemandAd, onWinnerFound: (OldAuctionResult?) -> Unit)

    fun saveAuction(demandAd: DemandAd, auction: Auction)
    fun isAuctionActive(demandAd: DemandAd): Boolean
    fun getResults(demandAd: DemandAd): List<OldAuctionResult>
}

