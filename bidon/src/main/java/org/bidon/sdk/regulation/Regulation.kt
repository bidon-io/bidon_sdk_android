package org.bidon.sdk.regulation

/**
 * Created by Aleksei Cherniaev on 21/06/2023.
 */
interface Regulation {
    /**
     * [GDPR](https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework/blob/master/TCFv2/IAB%20Tech%20Lab%20-%20CMP%20API%20v2.md#what-does-the-gdprapplies-value-mean)
     */
    var gdpr: Gdpr
    var gdprConsentString: String?
    val gdprApplies: Boolean
    val hasGdprConsent: Boolean

    /**
     * [CCPA US Privacy String](https://github.com/InteractiveAdvertisingBureau/USPrivacy/blob/master/CCPA/US%20Privacy%20String.md)
     */
    var usPrivacyString: String?
    val ccpaApplies: Boolean
    val hasCcpaConsent: Boolean

    /**
     * COPPA
     */
    var coppa: Coppa
    val coppaApplies: Boolean get() = coppa == Coppa.Yes
}
