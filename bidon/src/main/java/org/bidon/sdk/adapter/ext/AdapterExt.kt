package org.bidon.sdk.adapter.ext

import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.SupportsRegulation
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.ext.TAG

internal fun Adapter.applyRegulation() {
    val adapter = this
    (adapter as? SupportsRegulation)?.let {
        val regulation = BidonSdk.regulation
        logInfo(
            TAG,
            "Applying regulation to ${adapter.demandId.demandId} <- " +
                "GDPR=${regulation.gdpr}, " +
                "COPPA=${regulation.coppa}, " +
                "usPrivacyString=${regulation.usPrivacyString}, " +
                "gdprConsentString=${regulation.gdprConsentString}"
        )
        adapter.updateRegulation(regulation)
    }
}