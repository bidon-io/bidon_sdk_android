package org.bidon.unityads.ext

import com.unity3d.ads.UnityAds
import com.unity3d.services.banners.BannerErrorCode
import com.unity3d.services.banners.BannerErrorInfo
import org.bidon.sdk.config.BidonError
import org.bidon.unityads.UnityAdsDemandId

/**
 * Created by Bidon Team on 02/03/2023.
 */
internal fun UnityAds.UnityAdsLoadError?.asBidonError() = when (this) {
    UnityAds.UnityAdsLoadError.INTERNAL_ERROR -> BidonError.InternalServerSdkError("UnityAdsLoadError.INTERNAL_ERROR")
    UnityAds.UnityAdsLoadError.NO_FILL -> BidonError.NoFill(UnityAdsDemandId)
    UnityAds.UnityAdsLoadError.TIMEOUT -> BidonError.BidTimedOut(UnityAdsDemandId)
    UnityAds.UnityAdsLoadError.INITIALIZE_FAILED -> {
        BidonError.Unspecified(UnityAdsDemandId, Throwable("UnityAdsLoadError.INITIALIZE_FAILED"))
    }
    UnityAds.UnityAdsLoadError.INVALID_ARGUMENT -> BidonError.NoAppropriateAdUnitId
    null -> BidonError.Unspecified(UnityAdsDemandId)
}

internal fun UnityAds.UnityAdsShowError?.asBidonError() = when (this) {
    UnityAds.UnityAdsShowError.NOT_READY -> BidonError.AdNotReady
    UnityAds.UnityAdsShowError.NO_CONNECTION -> BidonError.NetworkError(UnityAdsDemandId)
    UnityAds.UnityAdsShowError.INVALID_ARGUMENT -> BidonError.NoAppropriateAdUnitId
    UnityAds.UnityAdsShowError.TIMEOUT -> BidonError.FillTimedOut(UnityAdsDemandId)
    UnityAds.UnityAdsShowError.VIDEO_PLAYER_ERROR,
    UnityAds.UnityAdsShowError.NOT_INITIALIZED,
    UnityAds.UnityAdsShowError.ALREADY_SHOWING,
    UnityAds.UnityAdsShowError.INTERNAL_ERROR,
    null -> {
        BidonError.Unspecified(UnityAdsDemandId, Throwable("$this"))
    }
}

internal fun BannerErrorInfo?.asBidonError() = when (this?.errorCode) {
    null -> BidonError.Unspecified(UnityAdsDemandId, Throwable("null"))
    BannerErrorCode.NATIVE_ERROR,
    BannerErrorCode.WEBVIEW_ERROR,
    BannerErrorCode.UNKNOWN -> BidonError.Unspecified(UnityAdsDemandId, Throwable(errorMessage))
    BannerErrorCode.NO_FILL -> BidonError.NoFill(UnityAdsDemandId)
}