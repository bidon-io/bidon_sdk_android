package org.bidon.sdk.auction.models

import org.bidon.sdk.adapter.AdSource
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
            return "AuctionResult.Network(ecpm=${adSource.getStats().ecpm}, roundStatus=$roundStatus, ${adSource.demandId})"
        }
    }

    class Bidding(
        override val adSource: AdSource<*>,
        override val roundStatus: RoundStatus,
    ) : AuctionResult {
        override fun toString(): String {
            return "AuctionResult.Bidding(ecpm=${adSource.getStats().ecpm}, roundStatus=$roundStatus, ${adSource.demandId})"
        }
    }

    data class BiddingLose(
        val adapterName: String,
        val ecpm: Double,
    ) : AuctionResult {
        override val roundStatus: RoundStatus = RoundStatus.Lose
        override val adSource: AdSource<*> get() = error("unexpected")
        override fun toString(): String {
            return "AuctionResult.Bidding($adapterName)"
        }
    }

    data class UnknownAdapter(
        val adapterName: String,
        val type: Type,
    ) : AuctionResult {
        override val roundStatus = RoundStatus.UnknownAdapter
        override val adSource: AdSource<*> get() = error("unexpected")

        enum class Type {
            Network,
            Bidding,
        }
    }
}