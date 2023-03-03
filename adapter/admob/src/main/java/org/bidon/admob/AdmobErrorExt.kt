package org.bidon.admob

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest.*
import com.google.android.gms.ads.LoadAdError
import org.bidon.sdk.config.BidonError

internal fun LoadAdError.asBidonError(): BidonError = when (this.code) {
    ERROR_CODE_NO_FILL,
    ERROR_CODE_MEDIATION_NO_FILL -> BidonError.NoBid(AdmobDemandId)
    ERROR_CODE_NETWORK_ERROR -> BidonError.NetworkError(AdmobDemandId)
    else -> BidonError.Unspecified(AdmobDemandId)
}

internal fun AdError.asBidonError(): BidonError = when (this.code) {
    ERROR_CODE_NO_FILL,
    ERROR_CODE_MEDIATION_NO_FILL -> BidonError.NoFill(AdmobDemandId)
    ERROR_CODE_NETWORK_ERROR -> BidonError.NetworkError(AdmobDemandId)
    else -> BidonError.Unspecified(AdmobDemandId)
}
