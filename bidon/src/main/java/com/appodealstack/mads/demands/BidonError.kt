package com.appodealstack.mads.demands

sealed class BidonError : Throwable()

sealed class DemandError(val demandId: DemandId? = null, val sourceError: Any? = null) : BidonError() {
    class Unspecified(demandId: DemandId?, sourceError: Any? = null) : DemandError(demandId)
    class NoFill(demandId: DemandId?, sourceError: Any? = null) : DemandError(demandId)
    class AdLoadFailed(demandId: DemandId?, sourceError: Any? = null) : DemandError(demandId)
    class NetworkError(demandId: DemandId?, sourceError: Any? = null) : DemandError(demandId)
    class NetworkTimeout(demandId: DemandId?, sourceError: Any? = null) : DemandError(demandId)
    class BadCredential(demandId: DemandId?, sourceError: Any? = null) : DemandError(demandId)
    class FullscreenAdAlreadyShowing(demandId: DemandId?, sourceError: Any? = null) : DemandError(demandId)
    class FullscreenAdNotReady(demandId: DemandId?, sourceError: Any? = null) : DemandError(demandId)
    class NoActivity(demandId: DemandId?, sourceError: Any? = null) : DemandError(demandId)
    class Expired(demandId: DemandId?, sourceError: Any? = null) : DemandError(demandId)
    class BannerSizeNotSupported(demandId: DemandId?, sourceError: Any? = null) : DemandError(demandId)
    class NoPlacement(demandId: DemandId?, sourceError: Any? = null) : DemandError(demandId)
}

sealed class AnalyticsError : BidonError()