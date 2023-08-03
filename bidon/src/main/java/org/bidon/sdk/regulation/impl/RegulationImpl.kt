package org.bidon.sdk.regulation.impl

import org.bidon.sdk.regulation.Coppa
import org.bidon.sdk.regulation.Gdpr
import org.bidon.sdk.regulation.Regulation

internal class RegulationImpl : Regulation {
    override var coppa: Coppa = Coppa.Default
    override var gdpr: Gdpr = Gdpr.Default

    override var gdprConsentString: String? = null
    override var usPrivacyString: String? = null
}