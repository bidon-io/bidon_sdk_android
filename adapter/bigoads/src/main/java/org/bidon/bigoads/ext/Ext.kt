package org.bidon.bigoads.ext

import org.bidon.bigoads.BigoAdsDemandId
import org.bidon.bigoads.BuildConfig
import org.bidon.sdk.config.BidonError
import sg.bigo.ads.BigoAdSdk
import sg.bigo.ads.api.AdError

/**
 * Created by Aleksei Cherniaev on 25/07/2023.
 */
internal var adapterVersion = BuildConfig.ADAPTER_VERSION
internal var sdkVersion = BigoAdSdk.getSDKVersionName()

internal fun AdError.asBidonError() = when (this.code) {
    AdError.ERROR_CODE_UNINITIALIZED -> BidonError.SdkNotInitialized

    AdError.ERROR_CODE_AD_DISABLE,
    // 1014 - The slot id is inactive or invalid, please make sure the id is aligned with app id.
    // If ids are correct, please wait for at least 30 minutes then try again.
    1014 -> BidonError.NoAppropriateAdUnitId

    AdError.ERROR_CODE_NETWORK_ERROR,
    AdError.ERROR_CODE_INVALID_REQUEST -> BidonError.NetworkError(BigoAdsDemandId, message)

    AdError.ERROR_CODE_NO_FILL -> BidonError.NoFill(BigoAdsDemandId)

    AdError.ERROR_CODE_INTERNAL_ERROR -> BidonError.InternalServerSdkError(message)

    AdError.ERROR_CODE_AD_EXPIRED -> BidonError.Expired(BigoAdsDemandId)

    AdError.ERROR_CODE_APP_ID_UNMATCHED -> BidonError.AppKeyIsInvalid

    AdError.ERROR_CODE_ASSETS_ERROR,
    AdError.ERROR_CODE_FULLSCREEN_AD_FAILED_TO_SHOW,
    AdError.ERROR_CODE_FULLSCREEN_AD_FAILED_TO_OPEN -> BidonError.AdNotReady

    else -> BidonError.Unspecified(
        demandId = BigoAdsDemandId,
        cause = Throwable("Message: $message. Code: $code")
    )
}
