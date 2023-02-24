package org.bidon.sdk.databinders.adapters

import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.databinders.DataBinder
import org.bidon.sdk.utils.json.jsonObject
import org.bidon.sdk.utils.serializer.serialize
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class AdaptersBinder(
    private val adaptersSource: AdaptersSource
) : DataBinder<JSONObject> {
    override val fieldName: String = "adapters"

    override suspend fun getJsonObject(): JSONObject = jsonObject {
        createDevice().forEach { (key, value) ->
            key hasValue value.serialize()
        }
    }

    private fun createDevice(): Map<String, AdapterInfo> {
        return adaptersSource.adapters.associate {
            it.demandId.demandId to it.adapterInfo
        }
    }
}