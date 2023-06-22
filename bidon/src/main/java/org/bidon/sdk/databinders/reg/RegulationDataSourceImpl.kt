package org.bidon.sdk.databinders.reg

import org.bidon.sdk.config.models.IabRequestBody
import org.bidon.sdk.config.models.RegulationsRequestBody
import org.bidon.sdk.regulation.Coppa
import org.bidon.sdk.regulation.Gdpr
import org.bidon.sdk.regulation.IabConsent
import org.bidon.sdk.regulation.Regulation

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
internal class RegulationDataSourceImpl(
    private val publisherRegulations: Regulation,
    private val iabConsent: IabConsent,
) : RegulationDataSource {
    override val regulationsRequestBody
        get() = RegulationsRequestBody(
            coppa = true.takeIf { publisherRegulations.coppa == Coppa.Yes } ?: false,
            gdpr = true.takeIf { publisherRegulations.gdpr == Gdpr.Given } ?: false,
            euPrivacy = publisherRegulations.gdprConsentString,
            usPrivacy = publisherRegulations.usPrivacyString,
            iab = iabConsent.iab.let {
                IabRequestBody(
                    tcfV1 = it.tcfV1,
                    tcfV2 = it.tcfV2,
                    usPrivacy = it.usPrivacy
                )
            }
        )
}