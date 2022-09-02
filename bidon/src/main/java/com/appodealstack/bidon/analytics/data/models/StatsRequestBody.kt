package com.appodealstack.bidon.analytics.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class StatsRequestBody(
    @SerialName("auction_id")
    val auctionId: String,
    @SerialName("auction_configuration_id")
    val auctionConfigurationId: Int,
    @SerialName("rounds")
    val rounds: List<Round>,
) {

    @Serializable
    data class Round(
        @SerialName("id")
        val id: Int,
        @SerialName("pricefloor")
        val pricefloor: Double,
        @SerialName("winner_id")
        val winnerDemandId: Double,
        @SerialName("winner_ecpm")
        val winnerEcpm: Double,
        @SerialName("demands")
        val demands: List<Demand>,
    )

    @Serializable
    data class Demand(
        @SerialName("id")
        val demandId: String,
        @SerialName("ad_unit_id")
        val adUnitId: String?,
        @SerialName("format")
        val adTypeFormatCode: String,
        @SerialName("status")
        val roundStatusCode: String,
        @SerialName("ecpm")
        val ecpm: Double?,
        @SerialName("start_ts")
        val startTs: Long,
        @SerialName("finish_ts")
        val finishTs: Long,
    ) {

        enum class RoundStatus(val code: Int) {
            Successful(1),
            NoBid(2),
            NoFill(3), // for Admob only NoBid possible
            UnknownAdapter(4),
            AdapterNotInitialized(5),
            BidTimeoutReached(6),
            FillTimeoutReached(7),
            NetworkError(8),
            IncorrectAdUnitId(9),
            NoAppropriateAdUnitId(10),
            AuctionCancelled(11),
            AdFormatNotSupported(12),
            UnspecifiedException(13),
        }
    }
}
