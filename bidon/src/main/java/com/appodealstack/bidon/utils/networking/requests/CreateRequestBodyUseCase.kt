package com.appodealstack.bidon.utils.networking.requests

import com.appodealstack.bidon.databinders.DataBinderType
import com.appodealstack.bidon.utils.json.JsonSerializer
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
