package org.bidon.sdk.auction

import org.bidon.sdk.config.BidonError

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface AuctionListener {
    fun onAuctionStarted() {}
    fun onAuctionSuccess(auctionResults: List<AuctionResult>) {}
    fun onAuctionFailed(cause: BidonError) {}
}
