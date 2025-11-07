package org.bidon.taurusx.ext

import com.taurusx.tax.api.TaurusXAdError
import com.taurusx.tax.api.TaurusXAdError.ERROR_CODE_NETWORK_ERROR
import com.taurusx.tax.api.TaurusXAdError.ERROR_CODE_NO_CONTENT
import com.taurusx.tax.api.TaurusXAdError.ERROR_CODE_NO_FILL
import com.taurusx.tax.api.TaurusXAdError.ERROR_CODE_SHOW
import com.taurusx.tax.api.TaurusXAdError.ERROR_CODE_TIMEOUT
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.config.BidonError
import org.bidon.taurusx.TaurusXAdFormat
import org.bidon.taurusx.TaurusXDemandId

internal fun TaurusXAdError?.asBidonError(): BidonError =
    if (this == null) {
        BidonError.Unspecified(TaurusXDemandId)
    } else {
        when (this.code) {
            ERROR_CODE_NETWORK_ERROR -> BidonError.NetworkError(TaurusXDemandId, message)
            ERROR_CODE_NO_CONTENT, ERROR_CODE_NO_FILL -> BidonError.NoFill(TaurusXDemandId)
            ERROR_CODE_TIMEOUT -> BidonError.FillTimedOut(TaurusXDemandId)
            ERROR_CODE_SHOW -> BidonError.AdNotReady
            else -> BidonError.Unspecified(TaurusXDemandId, Throwable(message))
        }
    }

internal fun AdTypeParam.toTaurusXAdFormat() = when (this) {
    is AdTypeParam.Banner -> {
        if (bannerFormat == BannerFormat.MRec) {
            TaurusXAdFormat.Mrec
        } else {
            TaurusXAdFormat.Banner
        }
    }

    is AdTypeParam.Interstitial -> TaurusXAdFormat.Interstitial
    is AdTypeParam.Rewarded -> TaurusXAdFormat.Rewarded
}
