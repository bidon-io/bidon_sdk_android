package org.bidon.sdk.auction.models

import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.stats.models.RoundStatus

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
sealed interface AuctionResult {
    val adSource: AdSource<*>
    val roundStatus: RoundStatus

    class Network(
        override val adSource: AdSource<*>,
        override val roundStatus: RoundStatus,
    ) : AuctionResult {
        override fun toString(): String {
            return "AuctionResult.Network(price=${adSource.getStats().price}, roundStatus=$roundStatus, ${adSource.demandId})"
        }
    }

    class Bidding(
        override val adSource: AdSource<*>,
        override val roundStatus: RoundStatus,
    ) : AuctionResult {
        override fun toString(): String {
            return "AuctionResult.Bidding(price=${adSource.getStats().price}, roundStatus=$roundStatus, ${adSource.demandId})"
        }
    }

    class AuctionFailed(
        val adUnit: AdUnit,
        val tokenInfo: TokenInfo?,
        override val roundStatus: RoundStatus,
    ) : AuctionResult {
        override val adSource: AdSource<*> get() = error("unexpected")
        override fun toString(): String {
            return "AuctionResult.${adUnit.getType()}(price=${adUnit.pricefloor}, roundStatus=$roundStatus, ${adUnit.demandId})"
        }
    }
}

private fun AdUnit.getType() = if (bidType == BidType.RTB) "Bidding" else "Network"