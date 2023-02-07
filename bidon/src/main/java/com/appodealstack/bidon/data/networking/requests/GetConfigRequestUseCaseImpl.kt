package com.appodealstack.bidon.data.networking.requests

import com.appodealstack.bidon.data.binderdatasources.segment.SegmentDataSource
import com.appodealstack.bidon.data.json.BidonJson
import com.appodealstack.bidon.data.models.config.ConfigRequestBody
import com.appodealstack.bidon.data.models.config.ConfigResponse
import com.appodealstack.bidon.data.networking.JsonHttpRequest
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.domain.config.usecases.GetConfigRequestUseCase
import com.appodealstack.bidon.domain.databinders.DataBinderType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement

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
        val requestBody = buildJsonObject {
            put("adapters", BidonJson.encodeToJsonElement(body.adapters))
            bindersData.forEach { (key, jsonObject) ->
                put(key, jsonObject)
            }
        }
        return get<JsonHttpRequest>().invoke(
            path = ConfigRequestPath,
            body = requestBody,
        ).mapCatching { jsonResponse ->
            /**
             * Save "segment_id"
             */
            segmentDataSource.saveSegmentId(segmentId = jsonResponse["segment_id"]?.toString())
            val config = jsonResponse.getValue("init")
            BidonJson.decodeFromJsonElement(ConfigResponse.serializer(), config)
        }
    }
}

private const val ConfigRequestPath = "config"