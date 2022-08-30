package com.appodealstack.bidon.config.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("idfa")
    var idfa: String,
    @SerialName("tracking_authorization_status")
    var trackingAuthorizationStatus: Int,
    @SerialName("idg")
    var idg: String?,
    @SerialName("consent")
    var consent: Consent,
    @SerialName("coppa")
    var coppa: Boolean
)

@Serializable
data class Consent(
    @SerialName("status")
    var status: String,
    @SerialName("acceptedVendors")
    var acceptedVendors: List<AcceptedVendors>?,
    @SerialName("vendorListVersion")
    var vendorListVersion: Int?,
    @SerialName("createdAt")
    var createdAt: Long?,
    @SerialName("updatedAt")
    var updatedAt: Long?,
    @SerialName("zone")
    var zone: String,
    @SerialName("iab")
    var iab: Iab
)

@Serializable
data class Iab(
    @SerialName("IABUSPrivacy_String")
    var IABUSPrivacyString: String?,
    @SerialName("IABConsent_ConsentString")
    var IABConsentConsentString: String?,
    @SerialName("IABConsent_ParsedPurposeConsents")
    var IABConsentParsedPurposeConsents: String?,
    @SerialName("IABConsent_ParsedVendorConsents")
    var IABConsentParsedVendorConsents: String?,
    @SerialName("IABConsent_SubjectToGDPR")
    var IABConsentSubjectToGDPR: String?
)

@Serializable
data class AcceptedVendors(
    @SerialName("apdId")
    var apdId: Int,
    @SerialName("status")
    var status: String
)