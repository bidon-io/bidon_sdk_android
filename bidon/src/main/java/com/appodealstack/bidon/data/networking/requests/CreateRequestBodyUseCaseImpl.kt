package com.appodealstack.bidon.data.networking.requests

import com.appodealstack.bidon.data.binderdatasources.DataProvider
import com.appodealstack.bidon.data.json.JsonSerializer
import com.appodealstack.bidon.data.json.jsonObject
import com.appodealstack.bidon.domain.databinders.DataBinderType
import com.appodealstack.bidon.domain.logging.impl.logInfo
import org.json.JSONObject

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
        dataSerializer: JsonSerializer<T>?,
    ): JSONObject {
        val bindData = dataProvider.provide(binders)
        return jsonObject {
            if (data != null && dataKeyName != null && dataSerializer != null) {
                dataKeyName hasValue dataSerializer.serialize(data)
            }
            bindData.forEach { (key, jsonElement) ->
                key hasValue jsonElement
            }
        }.also {
            logInfo(Tag, "$it")
        }
    }
}

private const val Tag = "CreateRequestBodyUseCase"