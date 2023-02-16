package com.appodealstack.bidon.adapter

import com.appodealstack.bidon.BidOnSdk
import com.appodealstack.bidon.ads.AdType

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class DemandAd(val adType: AdType, val placement: String = BidOnSdk.DefaultPlacement) {
    override fun toString(): String {
        return "DemandAd(adType=$adType, hashcode=${hashCode()})"
    }
}
