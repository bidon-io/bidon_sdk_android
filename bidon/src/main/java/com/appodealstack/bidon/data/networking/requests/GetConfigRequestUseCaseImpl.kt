package com.appodealstack.bidon.data.networking.requests

import com.appodealstack.bidon.data.binderdatasources.segment.SegmentDataSource
import com.appodealstack.bidon.data.json.JsonParsers
import com.appodealstack.bidon.data.json.jsonObject
import com.appodealstack.bidon.data.models.config.ConfigRequestBody
import com.appodealstack.bidon.data.models.config.ConfigResponse
import com.appodealstack.bidon.data.networking.JsonHttpRequest
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.domain.config.usecases.GetConfigRequestUseCase
import com.appodealstack.bidon.domain.databinders.DataBinderType
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