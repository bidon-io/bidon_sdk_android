package org.bidon.meta.ext

import com.facebook.ads.AdError
import com.facebook.ads.AdError.INTERSTITIAL_AD_TIMEOUT
import com.facebook.ads.AdError.NO_FILL_ERROR_CODE
import com.facebook.ads.BuildConfig
import org.bidon.meta.MetaDemandId
import org.bidon.sdk.config.BidonError

/**
 * Created by Aleksei Cherniaev on 08/08/2023.
 */
internal var adapterVersion = org.bidon.meta.BuildConfig.ADAPTER_VERSION
internal var sdkVersion = BuildConfig.VERSION_NAME

internal fun AdError?.asBidonError() = when (this?.errorCode) {
    NO_FILL_ERROR_CODE -> BidonError.NoFill(MetaDemandId)
    INTERSTITIAL_AD_TIMEOUT -> BidonError.Expired(MetaDemandId)
    else -> {
        BidonError.Unspecified(MetaDemandId, Throwable(this?.errorMessage))
    }
}
