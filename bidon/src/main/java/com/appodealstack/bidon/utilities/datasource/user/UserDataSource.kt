package com.appodealstack.bidon.utilities.datasource.user

import com.appodealstack.bidon.config.data.models.AcceptedVendors
import com.appodealstack.bidon.utilities.datasource.DataSource

internal interface UserDataSource : DataSource {
    // TODO temporary back Consent.toJson
    fun getConsent(): String
    fun getTrackingAuthorizationStatus(): Int
    fun getIdg(): String?
    // TODO does it need us?
    fun wasAdIdGenerated(): Boolean
    /**
     * @return Identifier for Advertisers. If it's not available by restrictions will return default
     * value (00000000-0000-0000-0000-000000000000).
     */
    fun getIdfa(): String
    fun getCoppa(): Boolean
    fun getConsentStatus(): String
    fun getAcceptedVendors(): List<AcceptedVendors>?
    fun getVendorListVersion(): Int?
    fun getConsentCreatedAt(): Long?
    fun getConsentUpdatedAt(): Long?
    fun getConsentZone(): String
    fun getIABUSPrivacyString(): String?
    fun getIABConsentConsentString(): String?
    fun getIABConsentParsedPurposeConsents(): String?
    fun getIABConsentParsedVendorConsents(): String?
    fun getIABConsentSubjectToGDPR(): String?
}