package org.bidon.inmobi.ext

import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiAdRequestStatus.StatusCode.*
import com.inmobi.sdk.InMobiSdk
import org.bidon.inmobi.BuildConfig
import org.bidon.inmobi.InmobiDemandId
import org.bidon.sdk.config.BidonError

/**
 * Created by Aleksei Cherniaev on 11/09/2023.
 */
internal var adapterVersion = BuildConfig.ADAPTER_VERSION
internal var sdkVersion = InMobiSdk.getVersion()

internal fun InMobiAdRequestStatus.asBidonError() =
    when (statusCode) {
        NETWORK_UNREACHABLE -> BidonError.NetworkError(InmobiDemandId, message)
        NO_FILL -> BidonError.NoFill(InmobiDemandId)
        AD_NO_LONGER_AVAILABLE -> BidonError.Expired(InmobiDemandId)
        REQUEST_TIMED_OUT -> BidonError.FillTimedOut(InmobiDemandId)
        else -> BidonError.Unspecified(
            InmobiDemandId,
            Throwable("Message: $message. Code: ${statusCode.name}")
        )
    }
