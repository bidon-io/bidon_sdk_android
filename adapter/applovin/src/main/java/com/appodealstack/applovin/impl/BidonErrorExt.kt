package com.appodealstack.applovin.impl

import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxErrorCode
import com.appodealstack.applovin.ApplovinMaxDemandId
import com.appodealstack.bidon.adapters.DemandError

internal fun MaxError.asBidonError(): DemandError = when (this.code) {
    MaxErrorCode.UNSPECIFIED -> DemandError.Unspecified(ApplovinMaxDemandId)
    MaxErrorCode.NO_FILL -> DemandError.NoFill(ApplovinMaxDemandId)
    MaxErrorCode.AD_LOAD_FAILED -> DemandError.AdLoadFailed(ApplovinMaxDemandId)
    MaxErrorCode.NO_NETWORK,
    MaxErrorCode.NETWORK_ERROR -> DemandError.NetworkError(ApplovinMaxDemandId)
    MaxErrorCode.NETWORK_TIMEOUT -> DemandError.NetworkTimeout(ApplovinMaxDemandId)
    MaxErrorCode.FULLSCREEN_AD_ALREADY_SHOWING -> DemandError.FullscreenAdAlreadyShowing(ApplovinMaxDemandId)
    MaxErrorCode.FULLSCREEN_AD_NOT_READY -> DemandError.FullscreenAdNotReady(ApplovinMaxDemandId)
    MaxErrorCode.NO_ACTIVITY -> DemandError.NoActivity(ApplovinMaxDemandId)
    else -> DemandError.Unspecified(ApplovinMaxDemandId)
}
