package org.bidon.moloco.ext

import com.moloco.sdk.publisher.Banner
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.MolocoAdError
import org.bidon.moloco.EMPTY_WATERMARK
import org.bidon.moloco.MolocoDemandId
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.config.BidonError

internal fun MolocoAdError.toBidonLoadError() = when (errorType) {
    MolocoAdError.ErrorType.AD_LOAD_FAILED_SDK_NOT_INIT,
    MolocoAdError.ErrorType.SDK_INIT_ERROR -> BidonError.SdkNotInitialized

    MolocoAdError.ErrorType.AD_LOAD_FAILED -> BidonError.NoFill(MolocoDemandId)
    MolocoAdError.ErrorType.AD_LOAD_TIMEOUT_ERROR -> BidonError.FillTimedOut(MolocoDemandId)
    MolocoAdError.ErrorType.AD_LOAD_BID_FAILED -> BidonError.NoFill(MolocoDemandId)
    else -> BidonError.Unspecified(MolocoDemandId, message = this.description)
}

internal fun MolocoAdError.toBidonShowError() = when (errorType) {
    MolocoAdError.ErrorType.AD_SHOW_ERROR_NOT_LOADED -> BidonError.AdNotReady
    else -> BidonError.Unspecified(MolocoDemandId, message = this.description)
}

internal fun Moloco.createBannerAd(
    bannerSize: BannerFormat,
    adUnitId: String,
    callback: (Banner?, Throwable?) -> Unit
) {
    val sdkCallback: (Banner?, MolocoAdError.AdCreateError?) -> Unit = { banner, err ->
        if (banner != null) {
            callback(banner, null)
        } else {
            val exception = Exception(
                "${bannerSize.name} wasn't created. " +
                    "Error: ${err?.description}, code: ${err?.errorCode}"
            )
            callback(null, exception)
        }
    }

    try {
        when (bannerSize) {
            BannerFormat.Banner ->
                Moloco.createBanner(adUnitId, EMPTY_WATERMARK, sdkCallback)

            BannerFormat.LeaderBoard ->
                Moloco.createBannerTablet(adUnitId, EMPTY_WATERMARK, sdkCallback)

            BannerFormat.MRec ->
                Moloco.createMREC(adUnitId, EMPTY_WATERMARK, sdkCallback)

            BannerFormat.Adaptive -> {
                callback(
                    null,
                    IllegalStateException("Adaptive format should have been resolved")
                )
            }
        }
    } catch (t: Throwable) {
        callback(null, t)
    }
}
