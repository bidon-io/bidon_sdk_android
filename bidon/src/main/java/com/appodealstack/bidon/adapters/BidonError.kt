package com.appodealstack.bidon.adapters

sealed class BidonError : Throwable() {
    object NoContextFound: BidonError()
    object AuctionFailed : BidonError()
    class NoFill(demandId: DemandId) : BidonError()
    class BidTimedOut(val demandId: DemandId) : BidonError()
    class FillTimedOut(val demandId: DemandId) : BidonError()
    object FullscreenAdNotReady : BidonError()
    object NoAppropriateAdUnitId : BidonError()
}

sealed class DemandError(val demandId: DemandId? = null) : BidonError() {
    class Unspecified(demandId: DemandId?) : DemandError(demandId)
    class NoFill(demandId: DemandId?) : DemandError(demandId)
    class AdLoadFailed(demandId: DemandId?) : DemandError(demandId)
    class NetworkError(demandId: DemandId?) : DemandError(demandId)
    class NetworkTimeout(demandId: DemandId?) : DemandError(demandId)
    class BadCredential(demandId: DemandId?) : DemandError(demandId)
    class FullscreenAdAlreadyShowing(demandId: DemandId?) : DemandError(demandId)
    class FullscreenAdNotReady(demandId: DemandId?) : DemandError(demandId)
    class NoActivity(demandId: DemandId?) : DemandError(demandId)
    class Expired(demandId: DemandId?) : DemandError(demandId)
    class BannerSizeNotSupported(demandId: DemandId?) : DemandError(demandId)
    class NoPlacement(demandId: DemandId?) : DemandError(demandId)
    class NoAppropriateAdUnitId(demandId: DemandId?) : DemandError(demandId)
}

sealed class AnalyticsError : BidonError()