package com.appodealstack.fyber.banner

import com.appodealstack.mads.demands.DemandError
import com.fyber.fairbid.ads.RequestFailure

internal fun RequestFailure?.asDemandError(): DemandError = when(this){
    RequestFailure.TIMEOUT -> DemandError.NetworkTimeout
    RequestFailure.NO_FILL -> DemandError.NoFill
    RequestFailure.BAD_CREDENTIALS -> DemandError.BadCredential
    RequestFailure.REMOTE_ERROR,
    RequestFailure.NETWORK_ERROR -> DemandError.NetworkError
    RequestFailure.UNAVAILABLE -> DemandError.Expired
    RequestFailure.ADAPTER_NOT_STARTED,
    RequestFailure.CANCELED,
    RequestFailure.NOT_YET_REQUESTED,
    RequestFailure.CONFIGURATION_ERROR,
    RequestFailure.SKIPPED,
    RequestFailure.CAPPED,
    RequestFailure.INTERNAL,
    RequestFailure.UNKNOWN,
    null -> DemandError.Unspecified
}