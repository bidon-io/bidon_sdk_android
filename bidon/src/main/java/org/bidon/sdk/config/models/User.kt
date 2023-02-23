package org.bidon.sdk.config.models

import org.bidon.sdk.utils.json.JsonParsers
import org.bidon.sdk.utils.json.JsonSerializer
import org.bidon.sdk.utils.json.jsonObject
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
data class User(
    var platformAdvertisingId: String, // idfa = iOS, AD_ID - Android.
    var trackingAuthorizationStatus: String,
    var applicationId: String?, // ID that app generates on the very first launch and send across session.
    var consent: Consent? = null, // TODO do not use it until ConsentManager is integrated
    var coppa: Boolean
)

internal class UserSerializer : JsonSerializer<User> {
    override fun serialize(data: User): JSONObject {
        return jsonObject {
            "idfa" hasValue data.platformAdvertisingId
            "tracking_authorization_status" hasValue data.trackingAuthorizationStatus
            "idg" hasValue data.applicationId
            "consent" hasValue JsonParsers.serializeOrNull(data.consent)
            "coppa" hasValue data.coppa
        }
    }
}

data class Consent(
    var status: String,
    var acceptedVendors: List<AcceptedVendors>?,
    var vendorListVersion: Int?,
    var createdAt: Long?,
    var updatedAt: Long?,
    var zone: String,
    var iab: Iab
)

data class Iab(
    var IABUSPrivacyString: String?,
    var IABConsentConsentString: String?,
    var IABConsentParsedPurposeConsents: String?,
    var IABConsentParsedVendorConsents: String?,
    var IABConsentSubjectToGDPR: String?
)

data class AcceptedVendors(
    var apdId: Int,
    var status: String
)