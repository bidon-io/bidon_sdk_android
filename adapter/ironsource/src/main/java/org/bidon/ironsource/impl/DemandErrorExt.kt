package org.bidon.ironsource.impl

import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.utils.IronSourceConstants
import org.bidon.ironsource.IronSourceDemandId
import org.bidon.sdk.config.BidonError

internal fun IronSourceError?.asBidonError(): BidonError = when (this?.errorCode) {
    IronSourceError.ERROR_CODE_NO_ADS_TO_SHOW,
    IronSourceError.ERROR_BN_LOAD_NO_FILL,
    IronSourceError.ERROR_RV_LOAD_FAILED_NO_CANDIDATES,
    IronSourceError.ERROR_IS_LOAD_FAILED_NO_CANDIDATES,
    IronSourceError.ERROR_RV_LOAD_NO_FILL,
    IronSourceError.ERROR_IS_LOAD_NO_FILL,
    IronSourceError.ERROR_BN_INSTANCE_LOAD_AUCTION_FAILED,
    IronSourceConstants.BN_INSTANCE_LOAD_NO_FILL,
    IronSourceError.ERROR_RV_SHOW_CALLED_DURING_SHOW,
    IronSourceError.ERROR_RV_SHOW_CALLED_WRONG_STATE,
    IronSourceError.ERROR_RV_LOAD_DURING_LOAD,
    IronSourceError.ERROR_RV_LOAD_DURING_SHOW,
    IronSourceError.ERROR_IS_SHOW_CALLED_DURING_SHOW,
    IronSourceError.ERROR_IS_LOAD_DURING_SHOW,
    IronSourceError.ERROR_DO_IS_LOAD_ALREADY_IN_PROGRESS,
    IronSourceError.ERROR_DO_RV_LOAD_ALREADY_IN_PROGRESS,
    IronSourceError.ERROR_DO_RV_LOAD_DURING_SHOW -> BidonError.NoFill(IronSourceDemandId)

    IronSourceError.ERROR_BN_INSTANCE_LOAD_TIMEOUT,
    IronSourceError.ERROR_BN_INSTANCE_RELOAD_TIMEOUT,
    IronSourceError.ERROR_RV_INIT_FAILED_TIMEOUT,
    IronSourceError.ERROR_RV_LOAD_FAIL_DUE_TO_INIT,
    IronSourceError.ERROR_DO_IS_LOAD_TIMED_OUT,
    IronSourceError.ERROR_DO_RV_LOAD_TIMED_OUT,
    IronSourceError.AUCTION_ERROR_TIMED_OUT,
    7113 -> BidonError.FillTimedOut(IronSourceDemandId)

    IronSourceError.ERROR_NO_INTERNET_CONNECTION -> BidonError.NetworkError(IronSourceDemandId)
    IronSourceError.ERROR_RV_EXPIRED_ADS -> BidonError.Expired(IronSourceDemandId)

    IronSourceError.ERROR_DO_IS_CALL_LOAD_BEFORE_SHOW,
    IronSourceError.ERROR_DO_RV_CALL_LOAD_BEFORE_SHOW,
    7202 -> BidonError.AdNotReady

    else -> BidonError.Unspecified(IronSourceDemandId)
}
