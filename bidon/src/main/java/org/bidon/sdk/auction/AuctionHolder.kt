package org.bidon.sdk.auction

import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.auction.models.AuctionResult

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal interface AuctionHolder : ExternalWinLossNotification {
    val isAuctionActive: Boolean

    fun startAuction(
        adTypeParam: AdTypeParam,
        onResult: (Result<List<AuctionResult>>) -> Unit
    )

    fun popWinnerForShow(): AdSource<*>?
    fun getNextLoadedWinner(): AdSource<*>?
    fun destroy()
    fun isAdReady(): Boolean
}