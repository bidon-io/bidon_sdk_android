package org.bidon.sdk.config.impl

import kotlinx.coroutines.withContext
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.config.models.ConfigRequestBody
import org.bidon.sdk.config.models.ConfigResponse
import org.bidon.sdk.config.usecases.GetConfigRequestUseCase
import org.bidon.sdk.databinders.DataBinderType
import org.bidon.sdk.databinders.segment.SegmentDataSource
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.json.JsonParsers
import org.bidon.sdk.utils.json.jsonObject
import org.bidon.sdk.utils.networking.JsonHttpRequest
import org.bidon.sdk.utils.networking.requests.CreateRequestBodyUseCase
import org.bidon.sdk.utils.serializer.serialize
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class GetConfigRequestUseCaseImpl(
    private val createRequestBody: CreateRequestBodyUseCase,
    private val segmentDataSource: SegmentDataSource,
) : GetConfigRequestUseCase {
    private val binders: List<DataBinderType> = listOf(
        DataBinderType.Device,
        DataBinderType.App,
        DataBinderType.Token,
        DataBinderType.Geo,
        DataBinderType.Session,
        DataBinderType.User,
    )

    override suspend fun request(body: ConfigRequestBody): Result<ConfigResponse> {
        return withContext(SdkDispatchers.IO) {
            val bindersData = createRequestBody(
                binders = binders,
                dataKeyName = null,
                data = null,
                extras = BidonSdk.getExtras()
            )
            val requestBody = jsonObject(putTo = bindersData) {
                "adapters" hasValue jsonObject {
                    body.adapters.forEach { (adapterName, data) ->
                        adapterName hasValue data.serialize()
                    }
                }
            }
            get<JsonHttpRequest>().invoke(
                path = ConfigRequestPath,
                body = requestBody,
            ).mapCatching { jsonString ->
                /**
                 * Save "segment_id"
                 */
                val jsonResponse = JSONObject(jsonString)
                segmentDataSource.saveSegmentId(
                    segmentId = jsonResponse.optString("segment_id", "").takeIf { !it.isNullOrBlank() }
                )
                val config = jsonResponse.getString("init")
                requireNotNull(JsonParsers.parseOrNull(config))
            }
        }
    }
}

private const val ConfigRequestPath = "config"
