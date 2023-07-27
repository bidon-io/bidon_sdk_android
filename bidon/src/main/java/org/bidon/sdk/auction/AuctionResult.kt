package org.bidon.sdk.auction

import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.stats.models.RoundStatus

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
sealed interface AuctionResult {
    val adSource: AdSource<*>
    val roundStatus: RoundStatus

    sealed interface Network : AuctionResult {
        class Success(
            override val adSource: AdSource<*>,
            override val roundStatus: RoundStatus,
        ) : Network {
            override fun toString(): String {
                return "AuctionResult.Network(ecpm=${adSource.getStats().ecpm}, roundStatus=$roundStatus, ${adSource.demandId})"
            }
        }

        data class UnknownAdapter(
            val adapterName: String
        ) : Network {
            override val roundStatus = RoundStatus.UnknownAdapter
            override val adSource: AdSource<*> get() = error("unexpected")
        }
    }

    sealed interface Bidding : AuctionResult {
        class Success(
            override val adSource: AdSource<*>,
            override val roundStatus: RoundStatus,
        ) : Bidding {
            override fun toString(): String {
                return "AuctionResult.Bidding.Success(ecpm=${adSource.getStats().ecpm}, roundStatus=$roundStatus, ${adSource.demandId})"
            }
        }

        class Failure(
            override val roundStatus: RoundStatus,
            override val adSource: AdSource<*>,
        ) : Bidding {
            override fun toString(): String {
                return "AuctionResult.Bidding.Failure(ecpm=${adSource.getStats().ecpm}, roundStatus=$roundStatus, ${adSource.demandId})"
            }
        }
    }
}