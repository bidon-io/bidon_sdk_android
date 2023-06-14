package org.bidon.sdk.databinders.reg

import org.bidon.sdk.config.models.Regulations

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
internal class RegulationDataSourceImpl : RegulationDataSource {
    override var regulations: Regulations = Regulations(
        coppa = false,
        gdpr = false
    )

    override fun setCoppa(coppa: Boolean) {
        regulations = regulations.copy(
            coppa = coppa
        )
    }

    override fun setGdpr(gdpr: Boolean) {
        regulations = regulations.copy(
            gdpr = gdpr
        )
    }
}