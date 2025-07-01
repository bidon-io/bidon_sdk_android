package org.bidon.sdk.databinders.user

import org.bidon.sdk.databinders.DataSource
/**
 * Created by Bidon Team on 06/02/2023.
 */
internal interface UserDataSource : DataSource {
    fun getTrackingAuthorizationStatus(): String
    fun getApplicationId(): String

    /**
     * @return Identifier for Advertisers. If it's not available by restrictions will return default
     * value (00000000-0000-0000-0000-000000000000).
     */
    fun getAdvertisingId(): String

    suspend fun getAppSetId(): String?
    suspend fun getAppSetIdScope(): String?

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