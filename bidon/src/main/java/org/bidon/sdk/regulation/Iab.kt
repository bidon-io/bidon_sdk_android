package org.bidon.sdk.regulation

/**
 * [Consent Management Provider](https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework/blob/master/CMP%20JS%20API%20v1.1%20Final.md)
 */
data class Iab(
    val tcfV1: String?,
    val tcfV2: String?,
    val usPrivacy: String?,
)