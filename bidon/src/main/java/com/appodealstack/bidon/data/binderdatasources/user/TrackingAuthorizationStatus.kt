package com.appodealstack.bidon.data.binderdatasources.user

internal enum class TrackingAuthorizationStatus(val code: Int) {
    // NotDetermined(0), iOS Specific
    Restricted(1),
    Denied(2),
    Authorized(3),
}