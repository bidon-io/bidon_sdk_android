package org.bidon.sdk.adapter

import org.bidon.sdk.ads.AdType
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.utils.di.get

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
class DemandAd(val adType: AdType) : Extras by get() {
    override fun toString(): String {
        return "DemandAd(adType=$adType, extras=${getExtras()}, hashcode=${hashCode()})"
    }
}
