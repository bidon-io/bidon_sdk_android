package com.appodealstack.bidon.data.binderdatasources.user

internal enum class TrackingAuthorizationStatus(val code: String) {
    // NotDetermined("NOT_DETERMINED"), iOS Specific
    Restricted("RESTRICTED"),
    Denied("DENIED"),
    Authorized("AUTHORIZED"),
}