package com.appodealstack.bidon.utilities.datasource.user

import android.content.Context
import com.appodealstack.bidon.utilities.datasource.user.toconsentlib.Consent
import org.json.JSONObject

internal interface Regulator {

    val isGDPRScope: Boolean
    val isCCPAScope: Boolean
    val managerConsent: Consent?
    val resolveConsent: Boolean
    val isServerConsent: Boolean
    val USPrivacyString: String?
    val IABConsentString: String?
    val approvedConsent: Consent?

    suspend fun receiveRegulatorData(
        context: Context,
        appKey: String,
    ): Consent

    fun getCoppa(): Boolean
    fun applyPublisherManagerConsent(publisherManagerConsent: Consent?): Boolean
    fun applyServerConsent(jsonObject: JSONObject)
    fun hasConsent(): Boolean
    fun hasConsentForVendor(vendorName: String?): Boolean
}