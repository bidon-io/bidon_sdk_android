package com.appodealstack.bidon.utilities.datasource.user

import android.content.Context
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.utilities.datasource.user.toconsentlib.Consent
import kotlinx.coroutines.flow.MutableStateFlow
//local - to receive data from CM lib
internal object LocalConsentData {

    private var advertisingInfo: AdvertisingInfo = get()
    private val currentProfile: AdvertisingInfoImpl.AdvertisingProfile
    get() = advertisingInfo.adProfileFlow.value

    val consentFlow = MutableStateFlow<Consent?>(null)

    private val regulator: Regulator by lazy { RegulatorProvider() }

    suspend fun init(
        appKey: String,
        context: Context,
    ) {
        consentFlow.emit(
            regulator.receiveRegulatorData(
                context = context,
                appKey = appKey
            )
        )
    }

    fun applyPublisherManagerConsent(publisherManagerConsent: Consent?) {
        regulator.applyPublisherManagerConsent(publisherManagerConsent)
    }

    fun isServerConsent(): Boolean = regulator.isServerConsent

    fun hasConsent(): Boolean =
        !currentProfile.isLimitAdTrackingEnabled && regulator.hasConsent()

    fun hasConsentForVendor(vendorName: String?): Boolean =
        regulator.hasConsentForVendor(vendorName)

    fun isGDPRScope(): Boolean = regulator.isGDPRScope

    fun isCCPAScope(): Boolean = regulator.isCCPAScope

    fun isUserGdprProtected(): Boolean = isGDPRScope() && !hasConsent()

    fun isUserCcpaProtected(): Boolean = isCCPAScope() && !hasConsent()

    fun isUserProtected(): Boolean = isUserGdprProtected() || isUserCcpaProtected()

    fun getCoppa(): Boolean = regulator.getCoppa()
}