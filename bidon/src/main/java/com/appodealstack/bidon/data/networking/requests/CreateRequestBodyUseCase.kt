package com.appodealstack.bidon.data.networking.requests

import com.appodealstack.bidon.data.json.JsonSerializer
import com.appodealstack.bidon.domain.databinders.DataBinderType
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface CreateRequestBodyUseCase {
    suspend operator fun <T> invoke(
        binders: List<DataBinderType>,
        dataKeyName: String?,
        data: T?,
        dataSerializer: JsonSerializer<T>?,
    ): JSONObject
}
