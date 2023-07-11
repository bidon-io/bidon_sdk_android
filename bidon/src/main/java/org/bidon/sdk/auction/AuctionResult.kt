package org.bidon.sdk.auction

import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.stats.models.RoundStatus

/**
 * Created by Bidon Team on 06/02/2023.
 */
class AuctionResult(
    val ecpm: Double,
    val adSource: AdSource<*>,
    val roundStatus: RoundStatus
) {
    override fun toString(): String {
        return "AuctionResult(ecpm=$ecpm, roundStatus=$roundStatus, adSource=${adSource.demandId.demandId})"
    }
}
