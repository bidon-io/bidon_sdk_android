package com.appodealstack.bidon.adapters

import com.appodealstack.bidon.BidOnSdk

class DemandAd(val adType: AdType, val placement: String = BidOnSdk.DefaultPlacement) {
    override fun toString(): String {
        return "DemandAd(adType=$adType, hashcode=${hashCode()})"
    }
}
