package com.appodealstack.bidmachine

import com.appodealstack.mads.demands.DemandError
import com.appodealstack.mads.demands.DemandId
import io.bidmachine.utils.BMError

internal fun BMError.asBidonError(demandId: DemandId): DemandError = when (this) {
    BMError.Request,
    BMError.Server,
    BMError.NoConnection -> DemandError.NetworkError(demandId)
    BMError.TimeoutError -> DemandError.NetworkTimeout(demandId)
    BMError.AlreadyShown -> DemandError.FullscreenAdAlreadyShowing(demandId)
    BMError.Expired -> DemandError.Expired(demandId)
    else -> DemandError.Unspecified(demandId)
}