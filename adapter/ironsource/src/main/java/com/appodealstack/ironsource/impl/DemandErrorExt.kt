package com.appodealstack.ironsource.impl

import com.appodealstack.bidon.adapters.BidonError
import com.appodealstack.bidon.adapters.DemandError
import com.appodealstack.ironsource.IronSourceDemandId
import com.ironsource.mediationsdk.logger.IronSourceError

internal fun IronSourceError?.asBidonError(): BidonError = when (this?.errorCode) {
    IronSourceError.ERROR_BN_LOAD_NO_FILL,
    IronSourceError.ERROR_RV_LOAD_NO_FILL,
    IronSourceError.ERROR_IS_LOAD_NO_FILL -> BidonError.NoFill(IronSourceDemandId)

    IronSourceError.ERROR_DO_RV_LOAD_MISSING_ACTIVITY,
    IronSourceError.ERROR_DO_IS_LOAD_MISSING_ACTIVITY,
    IronSourceError.ERROR_DO_BN_LOAD_MISSING_ACTIVITY -> BidonError.NoContextFound

    else -> DemandError.Unspecified(IronSourceDemandId)
}
