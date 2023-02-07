package com.appodealstack.bidon.domain.databinders

import com.appodealstack.bidon.data.json.BidonJson
import com.appodealstack.bidon.data.models.config.AdapterInfo
import com.appodealstack.bidon.domain.adapter.AdaptersSource
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
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