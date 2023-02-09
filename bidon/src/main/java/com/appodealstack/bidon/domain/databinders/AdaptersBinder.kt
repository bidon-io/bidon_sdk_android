package com.appodealstack.bidon.domain.databinders

import com.appodealstack.bidon.data.json.JsonParsers
import com.appodealstack.bidon.data.json.jsonObject
import com.appodealstack.bidon.data.models.config.AdapterInfo
import com.appodealstack.bidon.domain.adapter.AdaptersSource
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
            key hasValue JsonParsers.serialize(value)
        }
    }

    private fun createDevice(): Map<String, AdapterInfo> {
        return adaptersSource.adapters.associate {
            it.demandId.demandId to it.adapterInfo
        }
    }
}