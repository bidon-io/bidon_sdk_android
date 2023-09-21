package org.bidon.applovin.impl

import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxErrorCode
import org.bidon.applovin.ApplovinDemandId
import org.bidon.sdk.config.BidonError

internal fun MaxError.asBidonError(): BidonError = when (this.code) {
    MaxErrorCode.UNSPECIFIED -> BidonError.Unspecified(ApplovinDemandId)
    MaxErrorCode.NO_FILL -> BidonError.NoFill(ApplovinDemandId)
    MaxErrorCode.AD_LOAD_FAILED -> BidonError.NoBid(ApplovinDemandId)
    MaxErrorCode.NO_NETWORK,
    MaxErrorCode.NETWORK_ERROR -> BidonError.NetworkError(ApplovinDemandId)
    MaxErrorCode.NETWORK_TIMEOUT -> BidonError.NetworkError(ApplovinDemandId)
    MaxErrorCode.FULLSCREEN_AD_ALREADY_SHOWING -> BidonError.AdNotReady
    MaxErrorCode.FULLSCREEN_AD_NOT_READY -> BidonError.AdNotReady
    MaxErrorCode.NO_ACTIVITY -> BidonError.NoContextFound
    else -> BidonError.Unspecified(ApplovinDemandId)
}
