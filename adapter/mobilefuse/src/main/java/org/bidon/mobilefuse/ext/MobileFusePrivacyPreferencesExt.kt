package org.bidon.mobilefuse.ext

import com.mobilefuse.sdk.privacy.MobileFusePrivacyPreferences
import org.bidon.sdk.regulation.Regulation

/**
 * Created by Aleksei Cherniaev on 20/10/2023.
 */
internal fun Regulation.toMobileFusePrivacyPreferences(): MobileFusePrivacyPreferences {
    val regulation = this
    return MobileFusePrivacyPreferences.Builder().apply {
        if (regulation.coppaApplies) {
            this.setSubjectToCoppa(true)
        }
        if (regulation.gdprApplies) {
            this.setGppConsentString(regulation.gdprConsentString)
        }
        if (regulation.ccpaApplies) {
            this.setUsPrivacyConsentString(regulation.usPrivacyString)
        }
    }.build()
}