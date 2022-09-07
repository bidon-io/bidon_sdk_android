package com.appodealstack.bidon.data.networking.requests

import com.appodealstack.bidon.domain.databinders.DataBinderType
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.JsonObject

internal interface CreateRequestBodyUseCase {
    suspend operator fun <T> invoke(
        binders: List<DataBinderType>,
        dataKeyName: String?,
        data: T?,
        dataSerializer: SerializationStrategy<T>?,
    ): JsonObject
}
