package org.bidon.vkads.ext

import com.my.target.ads.MyTargetView.AdSize
import com.my.target.common.MyTargetVersion
import com.my.target.common.models.IAdLoadingError
import com.my.target.common.models.IAdLoadingError.LoadErrorType.INTERNAL_SERVER_ERROR
import com.my.target.common.models.IAdLoadingError.LoadErrorType.INVALID_BANNER_TYPE
import com.my.target.common.models.IAdLoadingError.LoadErrorType.INVALID_URL
import com.my.target.common.models.IAdLoadingError.LoadErrorType.NETWORK_CONNECTION_FAILED
import com.my.target.common.models.IAdLoadingError.LoadErrorType.REQUEST_TIMEOUT
import com.my.target.common.models.IAdLoadingError.LoadErrorType.REQUIRED_FIELD_MISSED
import com.my.target.common.models.IAdLoadingError.LoadErrorType.UNDEFINED_DATA_ERROR
import com.my.target.common.models.IAdLoadingError.LoadErrorType.UNDEFINED_PARSE_ERROR
import org.bidon.sdk.BuildConfig
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.DeviceInfo
import org.bidon.sdk.config.BidonError
import org.bidon.vkads.VkAdsDemandId

internal const val adapterVersion = BuildConfig.ADAPTER_VERSION
internal const val sdkVersion = MyTargetVersion.VERSION

internal fun BannerFormat.toAdSize() =
    when (this) {
        BannerFormat.LeaderBoard -> AdSize.ADSIZE_728x90
        BannerFormat.MRec -> AdSize.ADSIZE_300x250
        BannerFormat.Banner -> AdSize.ADSIZE_320x50
        BannerFormat.Adaptive -> if (DeviceInfo.isTablet) AdSize.ADSIZE_728x90 else AdSize.ADSIZE_320x50
    }

internal fun IAdLoadingError.asBidonError(bannerFormat: BannerFormat? = null): BidonError {
    return when (this.code) {
        NETWORK_CONNECTION_FAILED -> BidonError.NetworkError(VkAdsDemandId)
        REQUEST_TIMEOUT -> BidonError.FillTimedOut(VkAdsDemandId)
        INVALID_URL,
        INTERNAL_SERVER_ERROR,
        UNDEFINED_DATA_ERROR,
        UNDEFINED_PARSE_ERROR -> BidonError.Unspecified(VkAdsDemandId, Throwable(message))

        REQUIRED_FIELD_MISSED -> BidonError.IncorrectAdUnit(VkAdsDemandId, message)
        INVALID_BANNER_TYPE -> bannerFormat?.let {
            BidonError.AdFormatIsNotSupported(VkAdsDemandId.demandId, it)
        } ?: BidonError.Unspecified(VkAdsDemandId, Throwable(message))

        else -> BidonError.NoFill(VkAdsDemandId)
    }
}