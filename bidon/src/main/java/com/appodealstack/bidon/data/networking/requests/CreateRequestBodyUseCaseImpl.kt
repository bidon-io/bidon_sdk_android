package com.appodealstack.bidon.data.networking.requests

import com.appodealstack.bidon.data.binderdatasources.DataProvider
import com.appodealstack.bidon.data.json.BidonJson
import com.appodealstack.bidon.domain.databinders.DataBinderType
import com.appodealstack.bidon.domain.stats.impl.logInfo
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class CreateRequestBodyUseCaseImpl(
    private val dataProvider: DataProvider,
) : CreateRequestBodyUseCase {
    override suspend operator fun <T> invoke(
        binders: List<DataBinderType>,
        dataKeyName: String?,
        data: T?,
        dataSerializer: SerializationStrategy<T>?,
    ): JsonObject {
        val bindData = dataProvider.provide(binders)
        return buildJsonObject {
            if (data != null && dataKeyName != null && dataSerializer != null) {
                put(dataKeyName, BidonJson.encodeToJsonElement(dataSerializer, data))
            }
            bindData.forEach { (key, jsonElement) ->
                put(key, jsonElement)
            }
        }.also {
            logInfo("CreateRequestBodyUseCase", "$it")
        }
    }
}