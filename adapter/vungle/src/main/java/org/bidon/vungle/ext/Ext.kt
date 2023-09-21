package org.bidon.vungle.ext

import com.vungle.warren.error.VungleException
import org.bidon.sdk.config.BidonError
import org.bidon.vungle.BuildConfig
import org.bidon.vungle.VungleDemandId

/**
 * Created by Aleksei Cherniaev on 14/07/2023.
 */
internal var adapterVersion = BuildConfig.ADAPTER_VERSION
internal var sdkVersion = com.vungle.warren.BuildConfig.VERSION_NAME

internal fun VungleException?.asBidonError() = when (this?.exceptionCode) {
    VungleException.AD_EXPIRED -> BidonError.Expired(VungleDemandId)
    VungleException.APPLICATION_CONTEXT_REQUIRED -> BidonError.NoContextFound
    VungleException.VUNGLE_NOT_INTIALIZED -> BidonError.SdkNotInitialized
    VungleException.ALREADY_PLAYING_ANOTHER_AD -> BidonError.AdNotReady
    else -> BidonError.Unspecified(VungleDemandId)
}
