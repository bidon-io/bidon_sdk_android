package com.appodealstack.bidon.config.domain.databinders

import com.appodealstack.bidon.config.domain.DataBinder
import kotlinx.serialization.json.JsonElement

internal class GeoBinder : DataBinder {
    override val fieldName: String = "geo"

    override suspend fun getJsonElement(): JsonElement {
        TODO("Not yet implemented")
    }
}