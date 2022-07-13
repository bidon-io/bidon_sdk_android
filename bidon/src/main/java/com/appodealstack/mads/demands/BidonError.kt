package com.appodealstack.mads.demands

sealed class BidonError : Throwable()

sealed class DemandError : BidonError() {
    object Unspecified : DemandError()
    object NoFill : DemandError()
    object AdLoadFailed : DemandError()
    object NetworkError : DemandError()
    object NetworkTimeout : DemandError()
    object BadCredential : DemandError()
    object FullscreenAdAlreadyShowing : DemandError()
    object FullscreenAdNotReady : DemandError()
    object NoActivity : DemandError()
    object Expired : DemandError()
}

sealed class AnalyticsError : BidonError()