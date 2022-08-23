package com.appodealstack.bidon.utilities.restricted

import androidx.annotation.VisibleForTesting
import com.appodealstack.bidon.utilities.datasource.user.AdvertisingInfo
import com.appodealstack.bidon.utilities.datasource.user.LocalConsentData
import org.json.JSONObject

object RestrictableImpl : Restrictable {

    @VisibleForTesting
    private val defPersonalDataNotAllowedToCollect: Set<String> by lazy {
        setOf(
            PARAM_LT,
            PARAM_LAT,
            PARAM_LON,
            PARAM_AD_STATS,
            PARAM_USER_SETTINGS,
            PARAM_INAPPS
        )
    }

    private val personalDataNotAllowedToCollect: MutableSet<String> =
        HashSet(defPersonalDataNotAllowedToCollect)

    override fun isUserInGdprScope(): Boolean {
        return LocalConsentData.isGDPRScope()
    }

    override fun isUserInCcpaScope(): Boolean {
        return LocalConsentData.isCCPAScope()
    }

    override fun isUserHasConsent(): Boolean {
        return LocalConsentData.hasConsent()
    }

    override fun isUserGdprProtected(): Boolean {
        return LocalConsentData.isUserGdprProtected()
    }

    override fun isUserCcpaProtected(): Boolean {
        return LocalConsentData.isUserCcpaProtected()
    }

    override fun isUserProtected(): Boolean {
        return LocalConsentData.isUserProtected()
    }

    override fun canSendLocation(): Boolean {
        return !isParameterBlocked(PARAM_LAT)
                && !isParameterBlocked(PARAM_LON)
    }

    override fun canSendLocationType(): Boolean {
        return !isParameterBlocked(PARAM_LT)
    }

    override fun canSendUserSettings(): Boolean {
        return !isParameterBlocked(PARAM_USER_SETTINGS)
    }

    override fun isParameterBlocked(parameter: String?): Boolean {
        return isUserProtected() && containsBlockedParameter(parameter)
    }

    override fun isLimitAdTrackingEnabled(advertisingProfile: AdvertisingInfo.AdvertisingProfile): Boolean {
        return advertisingProfile.isLimitAdTrackingEnabled
    }

    /**
     * Function for parse gdpr and consent settings from response
     */
    //TODO receiving ServerConsent
    fun applyServerConsent(jObject: JSONObject) {
        // Remove all previous fields from not allowed to collect parameters
        personalDataNotAllowedToCollect.clear()
        if (jObject.has("gdpr")) {
            parseDataNotAllowedToCollect(jObject.optJSONObject("gdpr"))
        }
        if (jObject.has("ccpa")) {
            parseDataNotAllowedToCollect(jObject.optJSONObject("ccpa"))
        }
    }

    private fun containsBlockedParameter(parameter: String?): Boolean {
        return personalDataNotAllowedToCollect.contains(parameter)
    }

    /**
     * If "gdpr" or "ccpa" object is not null and "do_not_collect" field is not provided we shouldn't
     * collect all default fields. If "do_not_collect" field is provided we should filter out only
     * those fields which are provide or don't filter anything in case when "do_not_collect" field
     * is null or empty
     *
     * @param restrictedZoneObject
     */
    private fun parseDataNotAllowedToCollect(restrictedZoneObject: JSONObject?) {
        if (restrictedZoneObject != null && restrictedZoneObject.has("do_not_collect")) {
            val jsonArray = restrictedZoneObject.optJSONArray("do_not_collect")
            if (jsonArray != null) {
                for (i in 0 until jsonArray.length()) {
                    val param = jsonArray.optString(i, null)
                    if (param != null) {
                        personalDataNotAllowedToCollect.add(param)
                    }
                }
            }
        } else {
            personalDataNotAllowedToCollect.addAll(defPersonalDataNotAllowedToCollect)
        }
    }

    private const val PARAM_AD_STATS = "ad_stats"
    private const val PARAM_INAPPS = "inapps"
    private const val PARAM_LT = "lt"
    private const val PARAM_LAT = "lat"
    private const val PARAM_LON = "lon"
    private const val PARAM_USER_SETTINGS = "user_settings"
}