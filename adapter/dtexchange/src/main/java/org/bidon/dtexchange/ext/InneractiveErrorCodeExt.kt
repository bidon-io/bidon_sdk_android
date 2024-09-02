package org.bidon.dtexchange.ext

import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import org.bidon.dtexchange.DTExchangeDemandId
import org.bidon.sdk.config.BidonError

/**
 * Created by Bidon Team on 28/02/2023.
 */
internal fun InneractiveErrorCode?.asBidonError() = when (this) {
    InneractiveErrorCode.NO_FILL -> BidonError.NoFill(DTExchangeDemandId)

    InneractiveErrorCode.SERVER_INVALID_RESPONSE,
    InneractiveErrorCode.CONNECTION_ERROR,
    InneractiveErrorCode.SERVER_INTERNAL_ERROR -> {
        BidonError.NetworkError(DTExchangeDemandId, message = "InneractiveErrorCode: $this")
    }

    InneractiveErrorCode.CONNECTION_TIMEOUT,
    InneractiveErrorCode.LOAD_TIMEOUT -> BidonError.BidTimedOut(DTExchangeDemandId)

    InneractiveErrorCode.ERROR_CONFIGURATION_NO_SUCH_SPOT,
    InneractiveErrorCode.SPOT_DISABLED,
    InneractiveErrorCode.UNSUPPORTED_SPOT -> BidonError.NoAppropriateAdUnitId

    InneractiveErrorCode.UNKNOWN_APP_ID,
    InneractiveErrorCode.SDK_INTERNAL_ERROR,
    InneractiveErrorCode.CANCELLED,
    InneractiveErrorCode.IN_FLIGHT_TIMEOUT,
    InneractiveErrorCode.INVALID_INPUT,
    InneractiveErrorCode.ERROR_CODE_NATIVE_VIDEO_NOT_SUPPORTED,
    InneractiveErrorCode.NATIVE_ADS_NOT_SUPPORTED_FOR_OS,
    InneractiveErrorCode.ERROR_CONFIGURATION_MISMATCH,
    InneractiveErrorCode.NON_SECURE_CONTENT_DETECTED,
    InneractiveErrorCode.UNSPECIFIED,
    InneractiveErrorCode.SDK_NOT_INITIALIZED,
    InneractiveErrorCode.SDK_NOT_INITIALIZED_OR_CONFIG_ERROR -> {
        BidonError.Unspecified(DTExchangeDemandId, Throwable("InneractiveErrorCode: $this"))
    }
    else -> BidonError.Unspecified(DTExchangeDemandId, Throwable("InneractiveErrorCode: $this"))
}