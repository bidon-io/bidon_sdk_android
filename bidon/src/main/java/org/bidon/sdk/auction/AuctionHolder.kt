package org.bidon.sdk.auction

import org.bidon.sdk.adapter.AdSource

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
    fun isAdReady(): Boolean
}