package org.bidon.fyber.banner

import com.fyber.fairbid.ads.RequestFailure
import org.bidon.fyber.FairBidDemandId
import org.bidon.sdk.config.BidonError

internal fun RequestFailure?.asBidonError(): BidonError = when (this) {
    RequestFailure.TIMEOUT -> BidonError.NetworkError(FairBidDemandId)
    RequestFailure.NO_FILL -> BidonError.NoFill(FairBidDemandId)
    RequestFailure.BAD_CREDENTIALS -> BidonError.Unspecified(FairBidDemandId)
    RequestFailure.REMOTE_ERROR,
    RequestFailure.NETWORK_ERROR -> BidonError.NetworkError(FairBidDemandId)
    RequestFailure.UNAVAILABLE -> BidonError.Expired(FairBidDemandId)
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
