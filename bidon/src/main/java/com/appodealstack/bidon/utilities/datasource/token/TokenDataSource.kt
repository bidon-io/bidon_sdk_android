package com.appodealstack.bidon.utilities.datasource.token

import com.appodealstack.bidon.config.data.models.Token
import com.appodealstack.bidon.utilities.datasource.DataSource

internal interface TokenDataSource : DataSource {

    fun getCachedToken(): Token?
}