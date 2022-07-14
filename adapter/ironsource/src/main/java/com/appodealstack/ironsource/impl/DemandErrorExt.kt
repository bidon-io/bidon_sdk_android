package com.appodealstack.ironsource.impl

import com.appodealstack.ironsource.IronSourceDemandId
import com.appodealstack.mads.demands.DemandError
import com.ironsource.mediationsdk.logger.IronSourceError

internal fun IronSourceError?.asBidonError(): DemandError = when (this?.errorCode) {
    IronSourceError.ERROR_BN_LOAD_NO_FILL,
    IronSourceError.ERROR_RV_LOAD_NO_FILL,
    IronSourceError.ERROR_IS_LOAD_NO_FILL -> DemandError.NoFill(IronSourceDemandId, sourceError = this)

    IronSourceError.ERROR_DO_RV_LOAD_MISSING_ACTIVITY,
    IronSourceError.ERROR_DO_IS_LOAD_MISSING_ACTIVITY,
    IronSourceError.ERROR_DO_BN_LOAD_MISSING_ACTIVITY -> DemandError.NoActivity(IronSourceDemandId, sourceError = this)

    else -> DemandError.Unspecified(IronSourceDemandId, sourceError = this)
}