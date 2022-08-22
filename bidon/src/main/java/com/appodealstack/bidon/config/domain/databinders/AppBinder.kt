package com.appodealstack.bidon.config.domain.databinders

import com.appodealstack.bidon.config.domain.DataBinder
import kotlinx.serialization.json.JsonElement

internal class AppBinder : DataBinder {
    override val fieldName: String = "app"

    override suspend fun getJsonElement(): JsonElement {
        TODO("Not yet implemented")
    }
}
