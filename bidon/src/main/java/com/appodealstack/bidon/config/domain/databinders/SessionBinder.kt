package com.appodealstack.bidon.config.domain.databinders

import com.appodealstack.bidon.config.domain.DataBinder
import kotlinx.serialization.json.JsonElement

internal class SessionBinder : DataBinder {
    override val fieldName: String = "session"

    override suspend fun getJsonElement(): JsonElement {
        TODO("Not yet implemented")
    }
}
