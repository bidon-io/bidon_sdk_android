package org.bidon.sdk.databinders.token

import org.bidon.sdk.config.models.Token
import org.bidon.sdk.databinders.DataSource

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface TokenDataSource : DataSource {
    var token: Token?
}