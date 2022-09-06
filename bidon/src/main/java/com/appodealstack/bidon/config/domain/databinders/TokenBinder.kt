package com.appodealstack.bidon.config.domain.databinders

import com.appodealstack.bidon.config.domain.DataBinder
import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.utilities.datasource.token.TokenDataSource
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal class TokenBinder(
    private val dataSource: TokenDataSource
) : DataBinder {
    override val fieldName: String = "token"

    override suspend fun getJsonElement(): JsonElement = BidonJson.encodeToJsonElement(createToken())

    private fun createToken() =
        dataSource.getCachedToken()?.token?.let {
            BidonJson.parseToJsonElement(it)
        }
}
