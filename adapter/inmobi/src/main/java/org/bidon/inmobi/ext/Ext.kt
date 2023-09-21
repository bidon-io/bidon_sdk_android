package org.bidon.inmobi.ext

import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.sdk.InMobiSdk
import org.bidon.inmobi.BuildConfig
import org.bidon.inmobi.InmobiDemandId
import org.bidon.sdk.config.BidonError

/**
 * Created by Aleksei Cherniaev on 11/09/2023.
 */
internal var adapterVersion = BuildConfig.ADAPTER_VERSION
internal var sdkVersion = InMobiSdk.getVersion()

internal fun InMobiAdRequestStatus.asBidonError() = when (this.statusCode) {
    InMobiAdRequestStatus.StatusCode.NO_ERROR -> TODO()
    InMobiAdRequestStatus.StatusCode.NO_FILL -> BidonError.NoFill(InmobiDemandId)
    InMobiAdRequestStatus.StatusCode.REQUEST_INVALID -> TODO()
    InMobiAdRequestStatus.StatusCode.REQUEST_PENDING -> TODO()
    InMobiAdRequestStatus.StatusCode.REQUEST_TIMED_OUT -> BidonError.FillTimedOut(InmobiDemandId)
    InMobiAdRequestStatus.StatusCode.INTERNAL_ERROR -> TODO()
    InMobiAdRequestStatus.StatusCode.SERVER_ERROR,
    InMobiAdRequestStatus.StatusCode.NETWORK_UNREACHABLE -> BidonError.NetworkError(InmobiDemandId, this.message)
    InMobiAdRequestStatus.StatusCode.AD_ACTIVE -> TODO()
    InMobiAdRequestStatus.StatusCode.EARLY_REFRESH_REQUEST -> TODO()
    InMobiAdRequestStatus.StatusCode.AD_NO_LONGER_AVAILABLE -> TODO()
    InMobiAdRequestStatus.StatusCode.MISSING_REQUIRED_DEPENDENCIES -> TODO()
    InMobiAdRequestStatus.StatusCode.REPETITIVE_LOAD -> TODO()
    InMobiAdRequestStatus.StatusCode.GDPR_COMPLIANCE_ENFORCED -> TODO()
    InMobiAdRequestStatus.StatusCode.GET_SIGNALS_CALLED_WHILE_LOADING -> TODO()
    InMobiAdRequestStatus.StatusCode.LOAD_WITH_RESPONSE_CALLED_WHILE_LOADING -> TODO()
    InMobiAdRequestStatus.StatusCode.INVALID_RESPONSE_IN_LOAD -> TODO()
    InMobiAdRequestStatus.StatusCode.MONETIZATION_DISABLED -> TODO()
    InMobiAdRequestStatus.StatusCode.CALLED_FROM_WRONG_THREAD -> TODO()
    InMobiAdRequestStatus.StatusCode.CONFIGURATION_ERROR -> TODO()
    InMobiAdRequestStatus.StatusCode.LOW_MEMORY -> TODO()
}