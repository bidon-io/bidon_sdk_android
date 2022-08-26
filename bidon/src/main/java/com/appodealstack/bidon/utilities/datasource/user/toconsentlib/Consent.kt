package com.appodealstack.bidon.utilities.datasource.user.toconsentlib

import com.appodealstack.bidon.utilities.asList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.json.JSONArray
import org.json.JSONObject

/**
 * An object containing data about the user's consent to the collection and use of his personal data.
 */
//TODO should be in separate ConsentLibrary
data class Consent internal constructor(
    /**
     * Information about user consent [Status].
     */
    val status: Status = Status.UNKNOWN,
    /**
     * Consent [Zone] of regulation.
     */
    val zone: Zone = Zone.UNKNOWN,
    val version: Int = 0,
    private val payload: String = String(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val iab: JSONObject = JSONObject(), // TODO: 23.02.2022 [denis.glavatskikh] make private
    private val sdk: JSONObject = JSONObject(),
    val acceptedVendors: List<Vendor> = emptyList(),
) {
    internal constructor(json: JSONObject) : this(
        status = Status.values().find { it.name == json.optString("status") } ?: Status.UNKNOWN,
        zone = Zone.values().find { it.name == json.optString("zone") } ?: Zone.UNKNOWN,
        payload = json.optString("payload"),
        createdAt = json.optLong("createdAt"),
        updatedAt = json.optLong("updatedAt"),
        version = json.optInt("vendorListVersion"),
        iab = json.optJSONObject("iab") ?: JSONObject(),
        sdk = json.optJSONObject("sdk") ?: JSONObject(),
        acceptedVendors = json.optJSONArray("acceptedVendors")
            .asList<JSONObject>()
            .map { Vendor(it) },
    )

    /**
     * Information about user consent status.
     *
     * [UNKNOWN] The user has neither granted nor declined consent for personalized or non-personalized ads.
     * [NON_PERSONALIZED] The user has not granted consent for personalized ads.
     * [PARTLY_PERSONALIZED] The user has granted partly(for a few Ad networks) consent for personalized ads.
     * [PERSONALIZED] The user has granted consent for personalized ads.
     *
     */
    enum class Status {
        UNKNOWN, NON_PERSONALIZED, PARTLY_PERSONALIZED, PERSONALIZED
    }

    /**
     * Consent zone of regulation.
     *
     * [UNKNOWN] Consent data was not updated.
     * [NONE] The user does not fall under the laws restricting the collection and use of personal data.
     * [GDPR] User under GDPR law cover.
     * [CCPA] User under CCPA law cover.
     */
    enum class Zone {
        UNKNOWN, NONE, GDPR, CCPA
    }

    /**
     * Information about the necessary to show the consent form.
     *
     * [UNKNOWN] Consent data was not updated.
     * [TRUE] The user is within the scope of the GDPR or CCPA laws, the consent window should be displayed.
     * [FALSE] The user is not within the scope of the GDPR or CCPA laws OR the consent window has already been shown.
     */
    enum class ShouldShow {
        UNKNOWN, TRUE, FALSE
    }

    /**
     * Status for vendor:
     *
     * [UNKNOWN] Consent data was not updated.
     * [TRUE] The vendor's consent value is true.
     * [FALSE] The vendor's consent value is false.
     */
    enum class HasConsent {
        UNKNOWN, TRUE, FALSE
    }


    val isGDPRScope: Boolean get() = zone == Zone.GDPR
    val isCCPAScope: Boolean get() = zone == Zone.CCPA
    val booleanStatus: Boolean get() = status == Status.PERSONALIZED || status == Status.PARTLY_PERSONALIZED

    val IABConsentString: String get() = iab.optString("IABConsent_ConsentString")
    val USPrivacyString: String get() = iab.optString("IABUSPrivacy_String")
    val IABConsentSubjectToGDPR: String get() = iab.optString("IABConsent_SubjectToGDPR")
    val IABConsentParsedVendorConsents: String get() = iab.optString("IABConsent_ParsedVendorConsents")
    val IABConsentParsedPurposeConsents: String get() = iab.optString("IABConsent_ParsedPurposeConsents")

    fun hasConsentForVendor(bundle: String?): Boolean {
        bundle ?: return false
        val vendor = acceptedVendors.find { vendor -> vendor.bundle == bundle }
        return vendor != null
    }

    /**
     * Cast the [Consent] to the form [JSONObject].
     *
     * @return [JSONObject] of the [Consent] instance.
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("zone", zone)
            put("status", status)
            put("vendorListVersion", version.takeIf { it != 0 })
            put("payload", payload.takeIf { it.isNotEmpty() })
            put("createdAt", createdAt.takeIf { it != 0L })
            put("updatedAt", updatedAt.takeIf { it != 0L })
            put("iab", iab.takeIf { it.length() != 0 })
            put("sdk", sdk.takeIf { it.length() != 0 })
            put("acceptedVendors", JSONArray(acceptedVendors.map { vendor -> vendor.toJson() }))
        }
    }
}
