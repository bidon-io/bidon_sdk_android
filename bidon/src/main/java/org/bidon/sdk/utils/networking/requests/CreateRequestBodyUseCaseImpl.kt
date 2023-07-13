package org.bidon.sdk.utils.networking.requests

import org.bidon.sdk.databinders.DataBinderType
import org.bidon.sdk.databinders.DataProvider
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.json.jsonObject
import org.bidon.sdk.utils.serializer.Serializable
import org.bidon.sdk.utils.serializer.serialize
import org.json.JSONObject

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal class CreateRequestBodyUseCaseImpl(
    private val dataProvider: DataProvider,
) : CreateRequestBodyUseCase {
    override suspend operator fun <T : Serializable> invoke(
        binders: List<DataBinderType>,
        dataKeyName: String?,
        data: T?,
        list: List<T>,
        extras: Map<String, Any>
    ): JSONObject {
        val bindData = binders
            .takeIf { it.isNotEmpty() }
            ?.let { dataProvider.provide(binders) }
        return jsonObject {
            bindData?.forEach { (key, jsonElement) ->
                key hasValue jsonElement
            }
            if (extras.isNotEmpty()) {
                "ext" hasValue JSONObject(extras).toString()
            }
            if (dataKeyName != null) {
                when {
                    data != null -> {
                        dataKeyName hasValue data.serialize()
                    }
                    list.isNotEmpty() -> {
                        dataKeyName hasValue list.serialize()
                    }
                }
            }
        }.also {
            logInfo(Tag, "$it")
        }
    }
}

private const val Tag = "CreateRequestBodyUseCase"
