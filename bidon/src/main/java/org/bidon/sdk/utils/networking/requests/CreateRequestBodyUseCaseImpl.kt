package org.bidon.sdk.utils.networking.requests

import org.bidon.sdk.databinders.DataBinderType
import org.bidon.sdk.databinders.DataProvider
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.json.jsonObject
import org.bidon.sdk.utils.serializer.Serializable
import org.bidon.sdk.utils.serializer.serialize
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class CreateRequestBodyUseCaseImpl(
    private val dataProvider: DataProvider,
) : CreateRequestBodyUseCase {
    override suspend operator fun <T : Serializable> invoke(
        binders: List<DataBinderType>,
        dataKeyName: String?,
        data: T?,
    ): JSONObject {
        val bindData = dataProvider.provide(binders)
        return jsonObject {
            if (data != null && dataKeyName != null) {
                dataKeyName hasValue data.serialize()
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