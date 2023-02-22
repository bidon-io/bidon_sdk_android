package com.appodealstack.bidon.databinders.token

import com.appodealstack.bidon.databinders.DataBinder

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class TokenBinder(
    private val dataSource: TokenDataSource
) : DataBinder<String> {
    override val fieldName: String = "token"

    override suspend fun getJsonObject(): String? = dataSource.token?.token
}
