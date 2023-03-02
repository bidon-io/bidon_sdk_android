package org.bidon.unityads.ext

import com.unity3d.ads.UnityAds
import org.bidon.sdk.config.BidonError
import org.bidon.unityads.UnityAdsDemandId

/**
 * Created by Aleksei Cherniaev on 02/03/2023.
 */
internal fun UnityAds.UnityAdsLoadError?.asBidonError() = when (this) {
    UnityAds.UnityAdsLoadError.INTERNAL_ERROR -> BidonError.InternalServerSdkError("UnityAdsLoadError.INTERNAL_ERROR")
    UnityAds.UnityAdsLoadError.NO_FILL -> BidonError.NoFill(UnityAdsDemandId)
    UnityAds.UnityAdsLoadError.TIMEOUT -> BidonError.BidTimedOut(UnityAdsDemandId)
    UnityAds.UnityAdsLoadError.INITIALIZE_FAILED -> {
        BidonError.Unspecified(UnityAdsDemandId, Throwable("UnityAdsLoadError.INITIALIZE_FAILED"))
    }
    UnityAds.UnityAdsLoadError.INVALID_ARGUMENT -> {
        BidonError.Unspecified(UnityAdsDemandId, Throwable("UnityAdsLoadError.INVALID_ARGUMENT"))
    }
    null -> BidonError.Unspecified(UnityAdsDemandId)
}

internal fun UnityAds.UnityAdsShowError?.asBidonError() = when (this) {
    UnityAds.UnityAdsShowError.NOT_READY -> BidonError.FullscreenAdNotReady
    UnityAds.UnityAdsShowError.NO_CONNECTION -> BidonError.NetworkError(UnityAdsDemandId)
    UnityAds.UnityAdsShowError.INVALID_ARGUMENT,
    UnityAds.UnityAdsShowError.VIDEO_PLAYER_ERROR,
    UnityAds.UnityAdsShowError.NOT_INITIALIZED,
    UnityAds.UnityAdsShowError.ALREADY_SHOWING,
    UnityAds.UnityAdsShowError.INTERNAL_ERROR,
    UnityAds.UnityAdsShowError.TIMEOUT,
    null -> {
        BidonError.Unspecified(UnityAdsDemandId, Throwable("$this"))
    }
}