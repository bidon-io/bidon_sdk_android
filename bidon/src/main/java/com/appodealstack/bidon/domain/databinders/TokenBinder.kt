package com.appodealstack.bidon.domain.databinders

import com.appodealstack.bidon.data.binderdatasources.token.TokenDataSource

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class TokenBinder(
    private val dataSource: TokenDataSource
) : DataBinder<String> {
    override val fieldName: String = "token"

    override suspend fun getJsonObject(): String = dataSource.getCachedToken()?.token ?: ""
}
