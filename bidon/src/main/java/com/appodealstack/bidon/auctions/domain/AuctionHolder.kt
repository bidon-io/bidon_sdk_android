package com.appodealstack.bidon.auctions.domain

import com.appodealstack.bidon.auctions.data.models.AdTypeAdditional
import com.appodealstack.bidon.auctions.data.models.AuctionResult

internal interface AuctionHolder {
    val winner: AuctionResult?
    val isActive: Boolean

    fun startAuction(
        adTypeAdditional: AdTypeAdditional,
        onResult: (Result<List<AuctionResult>>) -> Unit
    )

    fun destroy()
}