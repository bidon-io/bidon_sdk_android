package com.appodealstack.mads.demands

/**
 * BidOn Errors
 */
sealed class BidOnError : Throwable()

sealed class DemandError : BidOnError() {
    object Unspecified : DemandError()
    object NoFill : DemandError()
    object AdLoadFailed : DemandError()
    object NetworkError : DemandError()
    object NetworkTimeout : DemandError()
    object FullscreenAdAlreadyShowing : DemandError()
    object FullscreenAdNotReady : DemandError()
    object NoActivity : DemandError()
}

sealed class AnalyticsError : BidOnError() {

}
