package com.appodealstack.bidmachine

import com.appodealstack.mads.demands.DemandError
import io.bidmachine.utils.BMError

internal fun BMError.asBidonError(): DemandError = when (this) {
    BMError.Request,
    BMError.Server,
    BMError.NoConnection -> DemandError.NetworkError
    BMError.TimeoutError -> DemandError.NetworkTimeout
    BMError.AlreadyShown -> DemandError.FullscreenAdAlreadyShowing
    BMError.Expired -> DemandError.Expired
    else -> DemandError.Unspecified
}