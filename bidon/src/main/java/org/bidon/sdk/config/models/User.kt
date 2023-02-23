package org.bidon.sdk.config.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal data class User(
    @field:JsonName("idfa")
    var platformAdvertisingId: String, // idfa = iOS, AD_ID - Android.
    @field:JsonName("tracking_authorization_status")
    var trackingAuthorizationStatus: String,
    @field:JsonName("idg")
    var applicationId: String?, // ID that app generates on the very first launch and send across session.
    @field:JsonName("consent")
    var consent: Consent? = null, // TODO do not use it until ConsentManager is integrated
    @field:JsonName("coppa")
    var coppa: Boolean
) : Serializable

internal data class Consent(
    var status: String,
    var acceptedVendors: List<AcceptedVendors>?,
    var vendorListVersion: Int?,
    var createdAt: Long?,
    var updatedAt: Long?,
    var zone: String,
    var iab: Iab
)

internal data class Iab(
    var IABUSPrivacyString: String?,
    var IABConsentConsentString: String?,
    var IABConsentParsedPurposeConsents: String?,
    var IABConsentParsedVendorConsents: String?,
    var IABConsentSubjectToGDPR: String?
)

internal data class AcceptedVendors(
    var apdId: Int,
    var status: String
)