package com.appodealstack.applovin.impl

import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxErrorCode
import com.appodealstack.applovin.ApplovinDemandId
import com.appodealstack.bidon.adapters.BidonError
import com.appodealstack.bidon.adapters.DemandError

internal fun MaxError.asBidonError(): BidonError = when (this.code) {
    MaxErrorCode.UNSPECIFIED -> BidonError.Unspecified(ApplovinDemandId)
    MaxErrorCode.NO_FILL -> BidonError.NoFill(ApplovinDemandId)
    MaxErrorCode.AD_LOAD_FAILED -> DemandError.AdLoadFailed(ApplovinDemandId)
    MaxErrorCode.NO_NETWORK,
    MaxErrorCode.NETWORK_ERROR -> BidonError.NetworkError(ApplovinDemandId)
    MaxErrorCode.NETWORK_TIMEOUT -> BidonError.NetworkError(ApplovinDemandId)
    MaxErrorCode.FULLSCREEN_AD_ALREADY_SHOWING -> BidonError.FullscreenAdNotReady
    MaxErrorCode.FULLSCREEN_AD_NOT_READY -> BidonError.FullscreenAdNotReady
    MaxErrorCode.NO_ACTIVITY -> BidonError.NoContextFound
    else -> BidonError.Unspecified(ApplovinDemandId)
}
