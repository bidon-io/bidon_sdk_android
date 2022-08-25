package com.appodealstack.applovin.impl

import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxErrorCode
import com.appodealstack.applovin.ApplovinDemandId
import com.appodealstack.bidon.adapters.DemandError

internal fun MaxError.asBidonError(): DemandError = when (this.code) {
    MaxErrorCode.UNSPECIFIED -> DemandError.Unspecified(ApplovinDemandId)
    MaxErrorCode.NO_FILL -> DemandError.NoFill(ApplovinDemandId)
    MaxErrorCode.AD_LOAD_FAILED -> DemandError.AdLoadFailed(ApplovinDemandId)
    MaxErrorCode.NO_NETWORK,
    MaxErrorCode.NETWORK_ERROR -> DemandError.NetworkError(ApplovinDemandId)
    MaxErrorCode.NETWORK_TIMEOUT -> DemandError.NetworkTimeout(ApplovinDemandId)
    MaxErrorCode.FULLSCREEN_AD_ALREADY_SHOWING -> DemandError.FullscreenAdAlreadyShowing(ApplovinDemandId)
    MaxErrorCode.FULLSCREEN_AD_NOT_READY -> DemandError.FullscreenAdNotReady(ApplovinDemandId)
    MaxErrorCode.NO_ACTIVITY -> DemandError.NoActivity(ApplovinDemandId)
    else -> DemandError.Unspecified(ApplovinDemandId)
}
