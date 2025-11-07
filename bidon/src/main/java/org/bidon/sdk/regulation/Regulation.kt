package org.bidon.sdk.regulation

/**
 * Created by Aleksei Cherniaev on 21/06/2023.
 */
public interface Regulation {
    /**
     * [GDPR](https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework/blob/master/TCFv2/IAB%20Tech%20Lab%20-%20CMP%20API%20v2.md#what-does-the-gdprapplies-value-mean)
     */
    public var gdpr: Gdpr
    public var gdprConsentString: String?
    public val gdprApplies: Boolean
    public val hasGdprConsent: Boolean

    /**
     * [CCPA US Privacy String](https://github.com/InteractiveAdvertisingBureau/USPrivacy/blob/master/CCPA/US%20Privacy%20String.md)
     */
    public var usPrivacyString: String?
    public val ccpaApplies: Boolean
    public val hasCcpaConsent: Boolean

    /**
     * COPPA
     */
    public var coppa: Coppa
    public val coppaApplies: Boolean get() = coppa == Coppa.Yes
}
