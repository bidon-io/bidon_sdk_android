package org.bidon.sdk.config.impl

import org.bidon.sdk.auction.models.AuctionCancellation
import org.bidon.sdk.config.BidonError

/**
 * Created by Bidon Team on 16/02/2023.
 */
internal fun Throwable.asBidonErrorOrUnspecified(): BidonError {
    return when (this) {
        is BidonError -> this
        is AuctionCancellation -> BidonError.AuctionCancelled
        else -> BidonError.Unspecified(demandId = null, cause = this)
    }
}
