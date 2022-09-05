package com.appodealstack.fyber.banner

import com.appodealstack.bidon.adapters.BidonError
import com.appodealstack.bidon.adapters.DemandError
import com.appodealstack.fyber.FairBidDemandId
import com.fyber.fairbid.ads.RequestFailure

internal fun RequestFailure?.asDemandError(): BidonError = when (this) {
    RequestFailure.TIMEOUT -> BidonError.NetworkError(FairBidDemandId)
    RequestFailure.NO_FILL -> BidonError.NoFill(FairBidDemandId)
    RequestFailure.BAD_CREDENTIALS -> DemandError.BadCredential(FairBidDemandId)
    RequestFailure.REMOTE_ERROR,
    RequestFailure.NETWORK_ERROR -> BidonError.NetworkError(FairBidDemandId)
    RequestFailure.UNAVAILABLE -> DemandError.Expired(FairBidDemandId)
    RequestFailure.ADAPTER_NOT_STARTED,
    RequestFailure.CANCELED,
    RequestFailure.NOT_YET_REQUESTED,
    RequestFailure.CONFIGURATION_ERROR,
    RequestFailure.SKIPPED,
    RequestFailure.CAPPED,
    RequestFailure.INTERNAL,
    RequestFailure.UNKNOWN,
    null -> BidonError.Unspecified(FairBidDemandId)
}
