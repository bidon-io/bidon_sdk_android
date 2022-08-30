package com.appodealstack.bidon.utilities.datasource.user

import com.appodealstack.bidon.config.data.models.AcceptedVendors
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.utilities.datasource.user.toconsentlib.Consent
import com.appodealstack.bidon.utilities.datasource.user.toconsentlib.Vendor

internal class UserDataSourceImpl(
    private val consentFactory: () -> Consent?
) : UserDataSource {

    private var advertisingInfo: AdvertisingInfo = get()
    private val currentProfile: AdvertisingInfoImpl.AdvertisingProfile
        get() = advertisingInfo.adProfileFlow.value

    private val isLimitAdTrackingEnabled: Boolean
        get() = currentProfile.isLimitAdTrackingEnabled

    private val consent get() = consentFactory()

    override fun getConsent(): String {
        return consent?.toJson().toString()
    }

    override fun getTrackingAuthorizationStatus(): Int {
        TODO()
    }

    // TODO is it neccessary value?
    override fun getIdg(): String? {
        return if (currentProfile.isAdvertisingIdWasGenerated) {
            currentProfile.id
        } else null
    }

    override fun wasAdIdGenerated(): Boolean {
        return currentProfile.isAdvertisingIdWasGenerated
    }

    override fun getIdfa(): String {
        return currentProfile.id
    }

    override fun getCoppa(): Boolean {
        return false
    }

    override fun getConsentStatus(): String {
        return consent?.status.toString()
    }

    override fun getAcceptedVendors(): List<AcceptedVendors>? {
        return consent?.acceptedVendors?.toAcceptedVendorList()
    }

    override fun getVendorListVersion(): Int? {
        return consent?.version
    }

    override fun getConsentCreatedAt(): Long? {
        return consent?.createdAt
    }

    override fun getConsentUpdatedAt(): Long? {
        return consent?.updatedAt
    }

    override fun getConsentZone(): String {
        return consent?.zone.toString()
    }

    override fun getIABUSPrivacyString(): String? {
        return consent?.USPrivacyString
    }

    override fun getIABConsentConsentString(): String? {
        return consent?.IABConsentString
    }

    override fun getIABConsentParsedPurposeConsents(): String? {
        return consent?.IABConsentParsedPurposeConsents
    }

    override fun getIABConsentParsedVendorConsents(): String? {
        return consent?.IABConsentParsedVendorConsents
    }

    override fun getIABConsentSubjectToGDPR(): String? {
        return consent?.IABConsentSubjectToGDPR
    }

    private fun List<Vendor>.toAcceptedVendorList(): List<AcceptedVendors> {
        return this.map { vendor ->
            AcceptedVendors(apdId = vendor.id, status = vendor.bundle)
        }
    }
}