package org.bidon.sdk.adapter

import org.bidon.sdk.BidOnSdk.DefaultPlacement
import org.bidon.sdk.ads.AdType

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class DemandAd(val adType: AdType, val placement: String = DefaultPlacement) {
    override fun toString(): String {
        return "DemandAd(adType=$adType, hashcode=${hashCode()})"
    }
}
