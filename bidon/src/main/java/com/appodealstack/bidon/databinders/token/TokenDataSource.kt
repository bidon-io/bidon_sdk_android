package com.appodealstack.bidon.databinders.token

import com.appodealstack.bidon.config.models.Token
import com.appodealstack.bidon.databinders.DataSource

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface TokenDataSource : DataSource {
    var token: Token?
}