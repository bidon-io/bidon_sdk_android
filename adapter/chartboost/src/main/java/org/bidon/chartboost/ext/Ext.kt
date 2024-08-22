package org.bidon.chartboost.ext

import com.chartboost.sdk.Chartboost
import com.chartboost.sdk.events.CacheError
import com.chartboost.sdk.events.ShowError
import org.bidon.chartboost.BuildConfig
import org.bidon.chartboost.ChartboostDemandId
import org.bidon.sdk.config.BidonError

internal var adapterVersion = BuildConfig.ADAPTER_VERSION
internal var sdkVersion = Chartboost.getSDKVersion()

internal fun CacheError?.asBidonLoadError(): BidonError = when (this?.code) {
    CacheError.Code.INTERNAL -> BidonError.Unspecified(ChartboostDemandId, exception)
    CacheError.Code.INTERNET_UNAVAILABLE -> BidonError.NetworkError(ChartboostDemandId, exception?.message)
    CacheError.Code.NETWORK_FAILURE -> BidonError.NetworkError(ChartboostDemandId, exception?.message)
    CacheError.Code.NO_AD_FOUND -> BidonError.NoFill(ChartboostDemandId)
    CacheError.Code.SESSION_NOT_STARTED -> BidonError.SdkNotInitialized
    CacheError.Code.SERVER_ERROR -> BidonError.Unspecified(ChartboostDemandId, exception)
    CacheError.Code.ASSET_DOWNLOAD_FAILURE -> BidonError.NoFill(ChartboostDemandId)
    CacheError.Code.BANNER_DISABLED -> BidonError.AdNotReady
    CacheError.Code.BANNER_VIEW_IS_DETACHED -> BidonError.Unspecified(ChartboostDemandId, exception)
    else -> BidonError.Unspecified(ChartboostDemandId)
}

internal fun ShowError?.asBidonShowError(): BidonError = when (this?.code) {
    ShowError.Code.INTERNAL -> BidonError.Unspecified(ChartboostDemandId, exception)
    ShowError.Code.SESSION_NOT_STARTED -> BidonError.SdkNotInitialized
    ShowError.Code.AD_ALREADY_VISIBLE -> BidonError.Unspecified(ChartboostDemandId, exception)
    ShowError.Code.INTERNET_UNAVAILABLE -> BidonError.NetworkError(ChartboostDemandId, exception?.message)
    ShowError.Code.PRESENTATION_FAILURE -> BidonError.AdNotReady
    ShowError.Code.NO_CACHED_AD -> BidonError.NoFill(ChartboostDemandId)
    ShowError.Code.BANNER_DISABLED -> BidonError.AdNotReady
    ShowError.Code.BANNER_VIEW_IS_DETACHED -> BidonError.Unspecified(ChartboostDemandId, exception)
    else -> BidonError.Unspecified(ChartboostDemandId)
}