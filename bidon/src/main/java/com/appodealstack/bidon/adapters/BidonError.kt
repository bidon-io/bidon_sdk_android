package com.appodealstack.bidon.adapters

import com.appodealstack.bidon.adapters.banners.BannerSize

sealed class BidonError : Throwable() {
    object NoContextFound : BidonError()
    object NoAuctionResults : BidonError()
    class NoFill(demandId: DemandId) : BidonError()
    class BidTimedOut(val demandId: DemandId) : BidonError()
    class FillTimedOut(val demandId: DemandId) : BidonError()
    class AdFormatIsNotSupported(val demandId: String, val bannerSize: BannerSize) : BidonError()
    class NetworkError(val demandId: DemandId?) : BidonError()
    class Unspecified(val demandId: DemandId?) : BidonError()

    object FullscreenAdNotReady : BidonError()
    object NoAppropriateAdUnitId : BidonError()
}

@Deprecated("use BidonError")
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
