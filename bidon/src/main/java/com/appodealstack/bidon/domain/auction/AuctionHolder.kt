package com.appodealstack.bidon.domain.auction

import com.appodealstack.bidon.domain.adapter.AdSource

internal interface AuctionHolder {
    val isActive: Boolean

    fun startAuction(
        adTypeParam: AdTypeParam,
        onResult: (Result<List<AuctionResult>>) -> Unit
    )

    fun popWinner(): AdSource<*>?
    fun destroy()
}