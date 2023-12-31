package org.bidon.sdk.databinders.token

import org.bidon.sdk.databinders.DataBinder

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal class TokenBinder(
    private val dataSource: TokenDataSource
) : DataBinder<String> {
    override val fieldName: String = "token"

    override suspend fun getJsonObject(): String? = dataSource.token?.token
}
