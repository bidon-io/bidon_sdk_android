package com.applovin.mediation.adapters.ext

import com.applovin.mediation.adapter.parameters.MaxAdapterParameters
import org.bidon.sdk.BidonSdk

internal fun BidonSdk.updatePrivacySettings(parameters: MaxAdapterParameters) {
    val bidonSdk = this
    // GDPR
    bidonSdk.regulation.gdprConsentString = parameters.consentString

    // CCPA
    val usPrivacyString = when (parameters.isDoNotSell) {
        true -> "1YY-"
        false -> "1YN-"
        else -> "1---"
    }
    bidonSdk.regulation.usPrivacyString = usPrivacyString
}
