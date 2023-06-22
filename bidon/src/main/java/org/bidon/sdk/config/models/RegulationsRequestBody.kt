package org.bidon.sdk.config.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
internal data class RegulationsRequestBody(
    @field:JsonName("coppa")
    val coppa: Boolean,
    @field:JsonName("gdpr")
    val gdpr: Boolean,
    @field:JsonName("us_privacy")
    val usPrivacy: String?,
    @field:JsonName("eu_privacy")
    val euPrivacy: String?,
    @field:JsonName("iab")
    val iab: IabRequestBody?,
) : Serializable

/**
 * [Mobile In-App Consent API](https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework/blob/master/Mobile%20In-App%20Consent%20APIs%20v1.0%20Final.md#how-do-third-party-sdks-vendors-access-the-consent-information-)
 */
internal data class IabRequestBody(
    @field:JsonName("tcf_v1")
    val tcfV1: String?,
    @field:JsonName("tcf_v2")
    val tcfV2: String?,
    @field:JsonName("us_privacy")
    val usPrivacy: String?,
) : Serializable