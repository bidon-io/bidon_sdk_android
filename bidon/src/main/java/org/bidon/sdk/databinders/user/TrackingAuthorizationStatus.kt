package org.bidon.sdk.databinders.user
/**
 * Created by Bidon Team on 06/02/2023.
 */
internal enum class TrackingAuthorizationStatus(val code: String) {
    // NotDetermined("NOT_DETERMINED"), iOS Specific
    Restricted("RESTRICTED"),
    Denied("DENIED"),
    Authorized("AUTHORIZED"),
}