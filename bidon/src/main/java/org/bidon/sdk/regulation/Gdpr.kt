package org.bidon.sdk.regulation

/**
 * Created by Aleksei Cherniaev on 21/06/2023.
 *
 * [GDPR](https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework/blob/master/TCFv2/IAB%20Tech%20Lab%20-%20CMP%20API%20v2.md#what-does-the-gdprapplies-value-mean)
 */
public enum class Gdpr(public val code: Int) {
    Unknown(code = -1), // unknown whether GDPR Applies
    DoesNotApply(code = 0), // GDPR Does not apply
    Applies(code = 1); // GDPR Applies

    public companion object {
        public val Default: Gdpr get() = Unknown
    }
}