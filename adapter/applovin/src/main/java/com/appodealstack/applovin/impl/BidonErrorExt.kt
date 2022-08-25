package com.appodealstack.applovin.impl

import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxErrorCode
import com.appodealstack.applovin.MaxDemandId
import com.appodealstack.bidon.adapters.DemandError

internal fun MaxError.asBidonError(): DemandError = when (this.code) {
    MaxErrorCode.UNSPECIFIED -> DemandError.Unspecified(MaxDemandId)
    MaxErrorCode.NO_FILL -> DemandError.NoFill(MaxDemandId)
    MaxErrorCode.AD_LOAD_FAILED -> DemandError.AdLoadFailed(MaxDemandId)
    MaxErrorCode.NO_NETWORK,
    MaxErrorCode.NETWORK_ERROR -> DemandError.NetworkError(MaxDemandId)
    MaxErrorCode.NETWORK_TIMEOUT -> DemandError.NetworkTimeout(MaxDemandId)
    MaxErrorCode.FULLSCREEN_AD_ALREADY_SHOWING -> DemandError.FullscreenAdAlreadyShowing(MaxDemandId)
    MaxErrorCode.FULLSCREEN_AD_NOT_READY -> DemandError.FullscreenAdNotReady(MaxDemandId)
    MaxErrorCode.NO_ACTIVITY -> DemandError.NoActivity(MaxDemandId)
    else -> DemandError.Unspecified(MaxDemandId)
}
