package com.appodealstack.bidon.config.domain.databinders

import com.appodealstack.bidon.config.data.models.AdapterInfo
import com.appodealstack.bidon.config.domain.DataBinder
import com.appodealstack.bidon.core.AdaptersSource
import com.appodealstack.bidon.core.BidonJson
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal class AdaptersBinder(
    private val adaptersSource: AdaptersSource
) : DataBinder {
    override val fieldName: String = "adapters"

    override suspend fun getJsonElement(): JsonElement =
        BidonJson.encodeToJsonElement(createDevice())

    private fun createDevice(): Map<String, AdapterInfo> {
        return adaptersSource.adapters.associate {
            it.demandId.demandId to it.adapterInfo
        }
    }
}