package org.bidon.sdk.utils.networking.requests

import org.bidon.sdk.databinders.DataBinderType
import org.bidon.sdk.utils.serializer.Serializable
import org.json.JSONObject

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal interface CreateRequestBodyUseCase {
    suspend operator fun <T : Serializable> invoke(
        binders: List<DataBinderType>,
        dataKeyName: String?,
        data: T?,
        extras: Map<String, Any>,
    ): JSONObject
}
