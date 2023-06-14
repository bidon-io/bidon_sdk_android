package org.bidon.sdk.ads.ext

import org.bidon.sdk.ads.AdType
import org.bidon.sdk.auction.AdTypeParam

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
internal fun AdTypeParam.asAdType() = when (this) {
    is AdTypeParam.Banner -> AdType.Banner
    is AdTypeParam.Interstitial -> AdType.Interstitial
    is AdTypeParam.Rewarded -> AdType.Rewarded
}