package com.appodealstack.bidon.databinders.adapters

import com.appodealstack.bidon.adapter.AdaptersSource
import com.appodealstack.bidon.config.models.AdapterInfo
import com.appodealstack.bidon.databinders.DataBinder
import com.appodealstack.bidon.utils.json.JsonParsers
import com.appodealstack.bidon.utils.json.jsonObject
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