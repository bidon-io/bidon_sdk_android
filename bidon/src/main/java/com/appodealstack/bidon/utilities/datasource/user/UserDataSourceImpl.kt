package com.appodealstack.bidon.utilities.datasource.user

import com.appodealstack.bidon.utilities.datasource.user.toconsentlib.Consent
import com.appodealstack.bidon.utilities.datasource.user.toconsentlib.Vendor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class UserDataSourceImpl(
    private val regulator: Regulator
) : UserDataSource {

    private var advertisingProfile: AdvertisingInfo.AdvertisingProfile =
        AdvertisingInfo.DefaultAdvertisingProfile

    private val consent: Consent?
        get() = regulator.approvedConsent

    private val isLimitAdTrackingEnabled: Boolean
        get() = advertisingProfile.isLimitAdTrackingEnabled

    init {
        CoroutineScope(Dispatchers.IO).launch {
            AdvertisingInfo.adProfileFlow.collect {
                advertisingProfile = it
                applyAdvertisingProfile(it)
            }
        }
    }

    //TODO receive AdvertisingProfile
    fun applyAdvertisingProfile(applyAdvertisingProfile: AdvertisingInfo.AdvertisingProfile) {
        if (isLimitAdTrackingEnabled != applyAdvertisingProfile.isLimitAdTrackingEnabled ||
            getIdfa() != applyAdvertisingProfile.id
        ) {
            advertisingProfile = applyAdvertisingProfile
        }
    }

    override fun getConsent(): String {
        return consent?.toJson().toString()
    }

    override fun getTrackingAuthorizationStatus(): Int {
        TODO(
            "Not yet implemented" +
                    "0 - Not determined\n" +
                    "1 - Restricted\n" +
                    "2 - Denied\n" +
                    "3 - Authorized"
        )
    }

    //TODO is it neccessary value?
    override fun getIdg(): String? {
        return if (advertisingProfile.isAdvertisingIdWasGenerated) {
            advertisingProfile.id
        } else null
    }

    override fun wasAdIdGenerated(): Boolean {
        return advertisingProfile.isAdvertisingIdWasGenerated
    }

    override fun getIdfa(): String {
        return advertisingProfile.id
    }

    override fun getCoppa(): Boolean {
        return LocalConsentData.getCoppa()
    }

    override fun getConsentStatus(): String {
        return consent?.status.toString()
    }

    override fun getAcceptedVendors(): List<Vendor>? {
        return consent?.acceptedVendors
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
}