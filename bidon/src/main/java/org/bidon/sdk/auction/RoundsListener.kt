package org.bidon.sdk.auction

import org.bidon.sdk.config.BidonError

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal interface RoundsListener {
    fun onRoundStarted(roundId: String, pricefloor: Double) {}
    fun onRoundSucceed(roundId: String, roundResults: List<AuctionResult>) {}
    fun onRoundFailed(roundId: String, cause: BidonError) {}
}
