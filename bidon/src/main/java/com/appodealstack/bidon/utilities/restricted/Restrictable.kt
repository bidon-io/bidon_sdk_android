package com.appodealstack.bidon.utilities.restricted

import com.appodealstack.bidon.utilities.datasource.user.AdvertisingInfo

interface Restrictable {

    /**
     * @return `true` when user is in GDPR scope, otherwise `false`
     */
    fun isUserInGdprScope(): Boolean

    /**
     * @return `true` when user is in CCPA scope, otherwise `false`
     */
    fun isUserInCcpaScope(): Boolean

    /**
     * @return `true` when user grant consent, otherwise `false`
     */
    fun isUserHasConsent(): Boolean

    /**
     * @return `true` when user data is protected by GDPR, otherwise `false`.
     * Consists of two checks: [Retractable.isUserInGdprScope] and
     * [Retractable.isUserHasConsent]
     */
    fun isUserGdprProtected(): Boolean

    /**
     * @return `true` when user data is protected by CCPA, otherwise `false`.
     * Consists of two checks: [Retractable.isUserInCcpaScope] and
     * [Retractable.isUserHasConsent]
     */
    fun isUserCcpaProtected(): Boolean

    /**
     * @return `true` when user data is protected by GDPR or CCPA, otherwise `false`.
     * Consists of two checks: [Retractable.isUserGdprProtected] and
     * [Retractable.isUserCcpaProtected]
     */
    fun isUserProtected(): Boolean

    /**
     * @return `true` when user info can be send to the third party sources
     */
    fun canSendUserSettings(): Boolean

    /**
     * @return `true` when device location can be send to the third party sources
     */
    fun canSendLocation(): Boolean

    /**
     * @return `true` when device location type can be send to the third party sources
     */
    fun canSendLocationType(): Boolean

    /**
     * @return `true` when parameter can't be send to the third party sources
     */
    fun isParameterBlocked(parameter: String?): Boolean

    /**
     * @return `true` when device isLimitAdTrackingEnabled
     */
    fun isLimitAdTrackingEnabled(advertisingProfile: AdvertisingInfo.AdvertisingProfile): Boolean
}