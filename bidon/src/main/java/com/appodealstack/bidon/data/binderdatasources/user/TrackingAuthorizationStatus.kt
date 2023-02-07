package com.appodealstack.bidon.data.binderdatasources.user
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal enum class TrackingAuthorizationStatus(val code: String) {
    // NotDetermined("NOT_DETERMINED"), iOS Specific
    Restricted("RESTRICTED"),
    Denied("DENIED"),
    Authorized("AUTHORIZED"),
}