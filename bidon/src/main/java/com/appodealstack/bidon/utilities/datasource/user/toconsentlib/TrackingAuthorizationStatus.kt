package com.appodealstack.bidon.utilities.datasource.user.toconsentlib

internal enum class TrackingAuthorizationStatus(val code: Int) {
    // NotDetermined(0), iOS Specific
    Restricted(1),
    Denied(2),
    Authorized(3),
}