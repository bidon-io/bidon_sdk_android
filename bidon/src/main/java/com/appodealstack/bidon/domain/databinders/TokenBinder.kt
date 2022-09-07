package com.appodealstack.bidon.domain.databinders

import com.appodealstack.bidon.data.binderdatasources.token.TokenDataSource
import com.appodealstack.bidon.data.json.BidonJson
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
