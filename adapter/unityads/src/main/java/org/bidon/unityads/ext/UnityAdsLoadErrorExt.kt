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
    null -> BidonError.Unspecified(UnityAdsDemandId)
    UnityAds.UnityAdsLoadError.INTERNAL_ERROR -> BidonError.InternalServerSdkError("UnityAdsLoadError.INTERNAL_ERROR")
    UnityAds.UnityAdsLoadError.NO_FILL -> BidonError.NoFill(UnityAdsDemandId)
    UnityAds.UnityAdsLoadError.TIMEOUT -> BidonError.BidTimedOut(UnityAdsDemandId)
    UnityAds.UnityAdsLoadError.INVALID_ARGUMENT -> BidonError.NoAppropriateAdUnitId
    else -> BidonError.Unspecified(demandId = UnityAdsDemandId, cause = Throwable(name))
}

internal fun UnityAds.UnityAdsShowError?.asBidonError() = when (this) {
    null -> BidonError.Unspecified(UnityAdsDemandId)
    UnityAds.UnityAdsShowError.NOT_READY -> BidonError.AdNotReady
    UnityAds.UnityAdsShowError.NO_CONNECTION -> BidonError.NetworkError(UnityAdsDemandId)
    UnityAds.UnityAdsShowError.INVALID_ARGUMENT -> BidonError.NoAppropriateAdUnitId
    UnityAds.UnityAdsShowError.TIMEOUT -> BidonError.FillTimedOut(UnityAdsDemandId)
    else -> BidonError.Unspecified(demandId = UnityAdsDemandId, cause = Throwable(name))
}

internal fun BannerErrorInfo?.asBidonError() = when (this?.errorCode) {
    null -> BidonError.Unspecified(UnityAdsDemandId)
    BannerErrorCode.NATIVE_ERROR,
    BannerErrorCode.WEBVIEW_ERROR,
    BannerErrorCode.NO_FILL -> BidonError.NoFill(UnityAdsDemandId)
    else -> BidonError.Unspecified(
        demandId = UnityAdsDemandId,
        cause = Throwable("Message: $errorMessage. Code: $errorCode")
    )
}