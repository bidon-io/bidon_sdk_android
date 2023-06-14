package org.bidon.sdk.databinders.reg

import org.bidon.sdk.config.models.Regulations
import org.bidon.sdk.databinders.DataSource

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
internal interface RegulationDataSource : DataSource {
    val regulations: Regulations

    fun setCoppa(coppa: Boolean)
    fun setGdpr(gdpr: Boolean)
}
