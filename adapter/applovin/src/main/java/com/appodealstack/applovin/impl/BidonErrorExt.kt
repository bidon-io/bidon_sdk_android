package com.appodealstack.applovin.impl

import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxErrorCode
import com.appodealstack.mads.demands.DemandError

internal fun MaxError.asBidonError(): DemandError = when (this.code) {
    MaxErrorCode.UNSPECIFIED -> DemandError.Unspecified
    MaxErrorCode.NO_FILL -> DemandError.NoFill
    MaxErrorCode.AD_LOAD_FAILED -> DemandError.AdLoadFailed
    MaxErrorCode.NO_NETWORK,
    MaxErrorCode.NETWORK_ERROR -> DemandError.NetworkError
    MaxErrorCode.NETWORK_TIMEOUT -> DemandError.NetworkTimeout
    MaxErrorCode.FULLSCREEN_AD_ALREADY_SHOWING -> DemandError.FullscreenAdAlreadyShowing
    MaxErrorCode.FULLSCREEN_AD_NOT_READY -> DemandError.FullscreenAdNotReady
    MaxErrorCode.NO_ACTIVITY -> DemandError.NoActivity
    else -> DemandError.Unspecified
}
