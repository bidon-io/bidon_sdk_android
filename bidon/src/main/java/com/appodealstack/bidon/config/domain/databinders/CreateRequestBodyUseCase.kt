package com.appodealstack.bidon.config.domain.databinders

import com.appodealstack.bidon.config.data.models.AdapterInfo
import com.appodealstack.bidon.config.domain.DataBinderType
import com.appodealstack.bidon.config.domain.DataProvider
import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.core.ext.logInfo
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement

internal interface CreateRequestBodyUseCase {
    suspend operator fun <T> invoke(
        binders: List<DataBinderType>,
        dataKeyName: String,
        data: T,
        adapters: Map<String, AdapterInfo>,
        dataSerializer: SerializationStrategy<T>,
    ): JsonObject
}

internal class CreateRequestBodyUseCaseImpl(
    private val dataProvider: DataProvider,
) : CreateRequestBodyUseCase {
    override suspend operator fun <T> invoke(
        binders: List<DataBinderType>,
        dataKeyName: String,
        data: T,
        adapters: Map<String, AdapterInfo>,
        dataSerializer: SerializationStrategy<T>,
    ): JsonObject {
        val bindData = dataProvider.provide(binders)
        return buildJsonObject {
            put(dataKeyName, BidonJson.encodeToJsonElement(dataSerializer, data))
            put("adapters", BidonJson.encodeToJsonElement(adapters))
            bindData.forEach { (key, jsonElement) ->
                put(key, jsonElement)
            }
        }.also {
            logInfo("CreateRequestBodyUseCase", "$it")
        }
    }
}