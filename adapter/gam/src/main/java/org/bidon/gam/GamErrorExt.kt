package org.bidon.gam

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest.ERROR_CODE_MEDIATION_NO_FILL
import com.google.android.gms.ads.AdRequest.ERROR_CODE_NETWORK_ERROR
import com.google.android.gms.ads.AdRequest.ERROR_CODE_NO_FILL
import com.google.android.gms.ads.LoadAdError
import org.bidon.sdk.config.BidonError

internal fun LoadAdError.asBidonError(): BidonError = when (this.code) {
    ERROR_CODE_NO_FILL,
    ERROR_CODE_MEDIATION_NO_FILL -> BidonError.NoFill(GamDemandId)
    ERROR_CODE_NETWORK_ERROR -> BidonError.NetworkError(GamDemandId)
    else -> BidonError.Unspecified(
        demandId = GamDemandId,
        cause = Throwable("Domain: $domain. Message: $message. Code: $code")
    )
}

internal fun AdError.asBidonError(): BidonError = when (this.code) {
    ERROR_CODE_NO_FILL,
    ERROR_CODE_MEDIATION_NO_FILL -> BidonError.NoFill(GamDemandId)
    ERROR_CODE_NETWORK_ERROR -> BidonError.NetworkError(GamDemandId)
    else -> BidonError.Unspecified(
        demandId = GamDemandId,
        cause = Throwable("Domain: $domain. Message: $message. Code: $code")
    )
}