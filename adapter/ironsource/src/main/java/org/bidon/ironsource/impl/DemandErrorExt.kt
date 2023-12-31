package org.bidon.ironsource.impl

import com.ironsource.mediationsdk.logger.IronSourceError
import org.bidon.ironsource.IronSourceDemandId
import org.bidon.sdk.config.BidonError

internal fun IronSourceError?.asBidonError(): BidonError = when (this?.errorCode) {
    IronSourceError.ERROR_BN_LOAD_NO_FILL,
    IronSourceError.ERROR_RV_LOAD_NO_FILL,
    IronSourceError.ERROR_IS_LOAD_NO_FILL -> BidonError.NoFill(IronSourceDemandId)

    IronSourceError.ERROR_DO_RV_LOAD_MISSING_ACTIVITY,
    IronSourceError.ERROR_DO_IS_LOAD_MISSING_ACTIVITY,
    IronSourceError.ERROR_DO_BN_LOAD_MISSING_ACTIVITY -> BidonError.NoContextFound

    else -> BidonError.Unspecified(IronSourceDemandId)
}
