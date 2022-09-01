package com.appodealstack.bidon.utilities.datasource.user

import com.appodealstack.bidon.utilities.datasource.DataSource

internal interface UserDataSource : DataSource {
    fun getTrackingAuthorizationStatus(): Int
    fun getApplicationId(): String
    /**
     * @return Identifier for Advertisers. If it's not available by restrictions will return default
     * value (00000000-0000-0000-0000-000000000000).
     */
    fun getAdvertisingId(): String

//    fun getCoppa(): Boolean
//    fun getConsent(): String
//    fun getConsentStatus(): String
//    fun getAcceptedVendors(): List<AcceptedVendors>?
//    fun getVendorListVersion(): Int?
//    fun getConsentCreatedAt(): Long?
//    fun getConsentUpdatedAt(): Long?
//    fun getConsentZone(): String
//    fun getIABUSPrivacyString(): String?
//    fun getIABConsentConsentString(): String?
//    fun getIABConsentParsedPurposeConsents(): String?
//    fun getIABConsentParsedVendorConsents(): String?
//    fun getIABConsentSubjectToGDPR(): String?
}