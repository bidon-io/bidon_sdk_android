package com.appodealstack.mads.demands

sealed class BidonError : Throwable()

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
}

sealed class AnalyticsError : BidonError()