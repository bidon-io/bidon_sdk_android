package org.bidon.vungle.ext

import com.vungle.ads.AdExpiredError
import com.vungle.ads.AdExpiredOnPlayError
import com.vungle.ads.AdNotLoadedCantPlay
import com.vungle.ads.AdPayloadError
import com.vungle.ads.AdResponseEmptyError
import com.vungle.ads.InvalidBidPayloadError
import com.vungle.ads.NetworkTimeoutError
import com.vungle.ads.NetworkUnreachable
import com.vungle.ads.SdkNotInitialized
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

internal fun VungleError?.asBidonError() = when (this) {
    is SdkNotInitialized -> BidonError.SdkNotInitialized
    is NetworkUnreachable,
    is NetworkTimeoutError -> BidonError.NetworkError(VungleDemandId)

    is AdNotLoadedCantPlay -> BidonError.AdNotReady
    is AdResponseEmptyError -> BidonError.NoFill(VungleDemandId)
    is AdPayloadError,
    is InvalidBidPayloadError -> BidonError.NoBid

    is AdExpiredError,
    is AdExpiredOnPlayError -> BidonError.Expired(VungleDemandId)

    else -> BidonError.Unspecified(VungleDemandId, this)
}