package org.bidon.vungle.ext

import com.vungle.ads.VungleAds
import com.vungle.ads.VungleError
import org.bidon.sdk.config.BidonError
import org.bidon.vungle.BuildConfig
import org.bidon.vungle.VungleDemandId

/**
 * Created by Aleksei Cherniaev on 14/07/2023.
 */
internal var adapterVersion = BuildConfig.ADAPTER_VERSION
internal var sdkVersion = VungleAds.getSdkVersion()

internal fun VungleError?.asBidonError() = when (this?.code) {
    VungleError.AD_EXPIRED -> BidonError.Expired(VungleDemandId)
    VungleError.SDK_NOT_INITIALIZED -> BidonError.SdkNotInitialized
    VungleError.ALREADY_PLAYING_ANOTHER_AD -> BidonError.AdNotReady
    else -> BidonError.Unspecified(VungleDemandId)
}
