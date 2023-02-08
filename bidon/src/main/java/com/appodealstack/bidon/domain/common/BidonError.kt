package com.appodealstack.bidon.domain.common

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
sealed class BidonError : Throwable() {
    object NoContextFound : BidonError()
    object NoAuctionResults : BidonError()
    object NoRoundResults : BidonError()
    class NoBid(val demandId: DemandId) : BidonError()
    class NoFill(val demandId: DemandId) : BidonError()
    class BidTimedOut(val demandId: DemandId) : BidonError()
    class FillTimedOut(val demandId: DemandId) : BidonError()
    class AdFormatIsNotSupported(val demandId: String, val bannerSize: BannerSize) : BidonError()
    class NetworkError(val demandId: DemandId?) : BidonError()
    class Unspecified(val demandId: DemandId?, val sourceError: Throwable? = null) : BidonError()

    object FullscreenAdNotReady : BidonError()
    object NoAppropriateAdUnitId : BidonError()

    class Expired(val demandId: DemandId?) : BidonError()
}

internal fun Throwable.asUnspecified(demandId: DemandId? = null) = BidonError.Unspecified(
    demandId = demandId,
    sourceError = this
)