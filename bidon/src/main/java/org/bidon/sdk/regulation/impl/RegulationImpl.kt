package org.bidon.sdk.regulation.impl

import org.bidon.sdk.regulation.Coppa
import org.bidon.sdk.regulation.Gdpr
import org.bidon.sdk.regulation.Regulation

internal class RegulationImpl : Regulation {
    override var coppa: Coppa = Coppa.Default

    override var gdpr: Gdpr = Gdpr.Default
    override var gdprConsentString: String? = null
    override val hasGdprConsent: Boolean get() = !gdprConsentString.isNullOrBlank()
    override val gdprApplies: Boolean get() = gdpr == Gdpr.Applies

    override var usPrivacyString: String? = null
    override val ccpaApplies: Boolean
        get() {
            val usPrivacyString = usPrivacyString?.takeIf { it.length == 4 } ?: return false
            return usPrivacyString[0] == '1' && usPrivacyString.drop(1).all { it != '-' }
        }

    override val hasCcpaConsent: Boolean
        get() {
            val usPrivacyString = usPrivacyString?.takeIf { it.length == 4 } ?: return false
            /**
             * The user has NOT made a choice to opt out of sale. (N)
             */
            return usPrivacyString[0] == '1' && usPrivacyString[2].uppercaseChar() == 'N'
        }
}
