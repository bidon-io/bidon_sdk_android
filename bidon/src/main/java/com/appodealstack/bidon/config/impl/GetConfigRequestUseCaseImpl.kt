package com.appodealstack.bidon.config.impl

import com.appodealstack.bidon.config.models.ConfigRequestBody
import com.appodealstack.bidon.config.models.ConfigResponse
import com.appodealstack.bidon.config.usecases.GetConfigRequestUseCase
import com.appodealstack.bidon.databinders.DataBinderType
import com.appodealstack.bidon.databinders.segment.SegmentDataSource
import com.appodealstack.bidon.utils.di.get
import com.appodealstack.bidon.utils.json.JsonParsers
import com.appodealstack.bidon.utils.json.jsonObject
import com.appodealstack.bidon.utils.networking.JsonHttpRequest
import com.appodealstack.bidon.utils.networking.requests.CreateRequestBodyUseCase
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
        val bindersData = createRequestBody(
            binders = binders,
            dataKeyName = null,
            data = null,
            dataSerializer = null,
        )
        val requestBody = jsonObject(putTo = bindersData) {
            "adapters" hasValue jsonObject {
                body.adapters.forEach { (adapterName, data) ->
                    adapterName hasValue JsonParsers.serialize(data)
                }
            }
        }
        return get<JsonHttpRequest>().invoke(
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

private const val ConfigRequestPath = "config"
