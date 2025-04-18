package org.bidon.sdk.config.impl

import kotlinx.coroutines.withContext
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.bidding.BiddingConfigSynchronizer
import org.bidon.sdk.config.models.ConfigRequestBody
import org.bidon.sdk.config.models.ConfigResponse
import org.bidon.sdk.config.usecases.GetConfigRequestUseCase
import org.bidon.sdk.databinders.DataBinderType
import org.bidon.sdk.segment.SegmentSynchronizer
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.json.JsonParsers
import org.bidon.sdk.utils.json.jsonObject
import org.bidon.sdk.utils.networking.JsonHttpRequest
import org.bidon.sdk.utils.networking.requests.CreateRequestBodyUseCase
import org.bidon.sdk.utils.serializer.serialize

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal class GetConfigRequestUseCaseImpl(
    private val createRequestBody: CreateRequestBodyUseCase,
    private val segmentSynchronizer: SegmentSynchronizer,
    private val biddingConfigSynchronizer: BiddingConfigSynchronizer
) : GetConfigRequestUseCase {
    private val binders: List<DataBinderType> = listOf(
        DataBinderType.Device,
        DataBinderType.App,
        DataBinderType.Token,
        DataBinderType.Session,
        DataBinderType.User,
        DataBinderType.Reg,
        DataBinderType.Test,
        DataBinderType.Segment,
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
                segmentSynchronizer.parseSegmentUid(jsonString)
                biddingConfigSynchronizer.parse(jsonString)
                requireNotNull(JsonParsers.parseOrNull<ConfigResponse>(jsonString))
            }
        }
    }
}

private const val ConfigRequestPath = "config"
