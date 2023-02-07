package com.appodealstack.bidon.data.binderdatasources.token

import com.appodealstack.bidon.data.binderdatasources.DataSource
import com.appodealstack.bidon.data.models.config.Token
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface TokenDataSource : DataSource {

    fun getCachedToken(): Token?
}