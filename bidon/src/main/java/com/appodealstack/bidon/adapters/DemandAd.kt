package com.appodealstack.bidon.adapters

class DemandAd(val adType: AdType, val placement: String? = null) {
    override fun toString(): String {
        return "DemandAd(adType=$adType, hashcode=${hashCode()})"
    }
}
