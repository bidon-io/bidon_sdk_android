package com.appodealstack.bidon.domain.auction

import com.appodealstack.bidon.domain.adapter.AdSource
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface AuctionHolder {
    val isActive: Boolean

    fun startAuction(
        adTypeParam: AdTypeParam,
        onResult: (Result<List<AuctionResult>>) -> Unit
    )

    fun popWinner(): AdSource<*>?
    fun destroy()
}