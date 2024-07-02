package org.bidon.mintegral.ext

import com.mbridge.msdk.out.MBConfiguration
import org.bidon.mintegral.BuildConfig
import org.bidon.mintegral.MintegralDemandId
import org.bidon.sdk.config.BidonError

/**
 * Created by Aleksei Cherniaev on 20/06/2023.
 */
internal var adapterVersion = BuildConfig.ADAPTER_VERSION
internal var sdkVersion = MBConfiguration.SDK_VERSION

/**
 * https://dev.mintegral.com/doc/index.html?file=sdk-m_sdk-android&lang=en#thedescriptionofreturnstatus
 */
internal fun String?.asBidonError(): BidonError =
    when (this) {
        "NO_ADS_SOURCE",
        "EXCEPTION_RETURN_EMPTY" -> BidonError.NoFill(MintegralDemandId)
        "EXCEPTION_TIMEOUT" -> BidonError.FillTimedOut(MintegralDemandId)
        else -> BidonError.Unspecified(MintegralDemandId, Throwable(this))
    }