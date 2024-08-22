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
    VungleError.ALREADY_PLAYING_ANOTHER_AD,
    VungleError.AD_UNABLE_TO_PLAY,
    VungleError.AD_IS_LOADING,
    VungleError.AD_IS_PLAYING -> BidonError.AdNotReady
    // Request failed with error: 10001, impression auctioned but unsold
    10001 -> BidonError.NoFill(VungleDemandId)
    else -> BidonError.Unspecified(VungleDemandId)
}
