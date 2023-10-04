package org.bidon.sdk.adapter

import org.bidon.sdk.ads.AdType
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.databinders.extras.ExtrasImpl

/**
 * Created by Bidon Team on 06/02/2023.
 */
class DemandAd(val adType: AdType) : Extras by ExtrasImpl() {
    override fun toString(): String {
        return "DemandAd(adType=$adType, extras=${getExtras()}, hashcode=${hashCode()})"
    }
}
