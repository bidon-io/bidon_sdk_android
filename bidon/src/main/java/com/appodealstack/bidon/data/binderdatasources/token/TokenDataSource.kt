package com.appodealstack.bidon.data.binderdatasources.token

import com.appodealstack.bidon.data.binderdatasources.DataSource
import com.appodealstack.bidon.data.models.config.Token

internal interface TokenDataSource : DataSource {

    fun getCachedToken(): Token?
}