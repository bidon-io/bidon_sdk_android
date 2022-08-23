package com.appodealstack.bidon.utilities.datasource.user

import android.content.Context
import com.appodealstack.bidon.utilities.datasource.user.toconsentlib.Consent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

//local - to solve data from local CM Lib and server values
internal class RegulatorProvider : Regulator {

    private var isServerGDPRScope = false
    private var isServerCCPAScope = false
    private var childDirectedTreatment: Boolean? = null
    private var serverChildDirectedTreatment: Boolean? = null

    init {
        CoroutineScope(Dispatchers.IO).launch {
            LocalConsentData.consentFlow.collect {
                approvedConsent = it
            }
        }
    }

    override var approvedConsent: Consent? = null

    override var managerConsent: Consent? = null
        private set

    override val resolveConsent: Boolean
        get() = approvedConsent?.booleanStatus ?: managerConsent?.booleanStatus ?: false

    override var isServerConsent = false
        private set

    override val isGDPRScope: Boolean
        get() = approvedConsent?.isGDPRScope ?: managerConsent?.isGDPRScope ?: isServerGDPRScope

    override val isCCPAScope: Boolean
        get() = approvedConsent?.isCCPAScope ?: managerConsent?.isCCPAScope ?: isServerCCPAScope

    override val IABConsentString: String?
        get() = approvedConsent?.IABConsentString ?: managerConsent?.IABConsentString

    override val USPrivacyString: String?
        get() = approvedConsent?.USPrivacyString ?: managerConsent?.USPrivacyString

    override suspend fun receiveRegulatorData(
        context: Context,
        appKey: String,
    ): Consent {
        //TODO change logic according server and local Consent, need CM lib
//        val userConsent = userConsent
//        val (status, zone) = when {
//            userConsent != null -> {
//                userConsent.status to userConsent.zone
//            }
//            else -> {
//                null to null
//            }
//        }
//        val consentSdk = ConsentSdkFactory.createInstance(context)
//        val approvedConsent = consentSdk.getRegulatorData(
//            appKey = appKey,
//            publisherConsent = managerConsent,
//            status = status,
//            zone = zone
//        ) ?: ConsentManager.consent
//        this.approvedConsent = approvedConsent
        return approvedConsent!!
    }

    override fun applyPublisherManagerConsent(publisherManagerConsent: Consent?): Boolean {
        return if (managerConsent != publisherManagerConsent) {
            managerConsent = publisherManagerConsent
            true
        } else {
            false
        }
    }

    override fun applyServerConsent(jsonObject: JSONObject) {
        isServerGDPRScope = jsonObject.has(GDPR)
        isServerCCPAScope = jsonObject.has(CCPA)
        isServerConsent = jsonObject.optBoolean(CONSENT, true)
    }

    override fun hasConsentForVendor(vendorName: String?): Boolean {
        return isServerConsent && (managerConsent?.hasConsentForVendor(vendorName) ?: hasConsent())
    }

    override fun hasConsent(): Boolean = isServerConsent && resolveConsent

    override fun getCoppa(): Boolean {
        // In case when publisher provided childDirectedTreatment we should use it as primary
        return if (childDirectedTreatment != null) childDirectedTreatment!! else if (serverChildDirectedTreatment != null) serverChildDirectedTreatment!! else false
    }

    //TODO parse coppa from server
    fun parseCoppa(jsonObject: JSONObject?) {
        jsonObject?.also {
            if (it.has(COPPA)) {
                val previousState: Boolean = getCoppa()
                serverChildDirectedTreatment = it.optBoolean(COPPA, false)
                if (previousState != getCoppa()) {
                    //TODO refresh waterfalls?
                }
            }
        }
    }

    //TODO setCoppa from user
    fun setChildDirectedTreatment(value: Boolean) {
        val previousState: Boolean = getCoppa()
        childDirectedTreatment = value
        if (previousState != getCoppa()) {
            //TODO refresh waterfalls?
        }
    }

    companion object {
        const val GDPR = "gdpr"
        const val CCPA = "ccpa"
        const val CONSENT = "consent"
        const val COPPA = "coppa"
    }
}