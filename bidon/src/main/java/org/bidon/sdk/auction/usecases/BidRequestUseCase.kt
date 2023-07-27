package org.bidon.sdk.auction.usecases

import kotlinx.coroutines.withContext
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.ads.banner.helper.GetOrientationUseCase
import org.bidon.sdk.ads.ext.asAdRequestBody
import org.bidon.sdk.ads.ext.asAdType
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.BidRequestBody
import org.bidon.sdk.auction.models.BidResponse
import org.bidon.sdk.databinders.DataBinderType
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.json.JsonParsers
import org.bidon.sdk.utils.networking.JsonHttpRequest
import org.bidon.sdk.utils.networking.requests.CreateRequestBodyUseCase
import java.util.UUID

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
internal interface BidRequestUseCase {
    suspend fun invoke(
        adTypeParam: AdTypeParam,
        tokens: List<Pair<DemandId, String>>,
        extras: Map<String, Any>,
        bidfloor: Double,
        auctionId: String,
        roundId: String,
        auctionConfigurationId: Int?,
    ): Result<BidResponse>
}

internal class BidRequestUseCaseImpl(
    private val createRequestBody: CreateRequestBodyUseCase,
    private val getOrientation: GetOrientationUseCase,
) : BidRequestUseCase {
    private val binders: List<DataBinderType> = listOf(
        DataBinderType.AvailableAdapters,
        DataBinderType.Device,
        DataBinderType.App,
        DataBinderType.Token,
        DataBinderType.Session,
        DataBinderType.User,
        DataBinderType.Segment,
        DataBinderType.Reg,
        DataBinderType.Test,
    )

    override suspend fun invoke(
        adTypeParam: AdTypeParam,
        tokens: List<Pair<DemandId, String>>,
        extras: Map<String, Any>,
        bidfloor: Double,
        auctionId: String,
        roundId: String,
        auctionConfigurationId: Int?,
    ): Result<BidResponse> {
        return withContext(SdkDispatchers.IO) {
            val (banner, interstitial, rewarded) = adTypeParam.asAdRequestBody()
            val bidRequestBody = BidRequestBody(
                auctionId = auctionId,
                impressionId = UUID.randomUUID().toString(),
                demands = tokens.associate { (demandId, token) ->
                    demandId.demandId to BidRequestBody.Token(token)
                },
                bidfloor = bidfloor,
                orientationCode = getOrientation().code,
                roundId = roundId,
                auctionConfigurationId = auctionConfigurationId,
                banner = banner,
                interstitial = interstitial,
                rewarded = rewarded,
            )
            val requestBody = createRequestBody(
                binders = binders,
                dataKeyName = "imp",
                data = bidRequestBody,
                extras = BidonSdk.getExtras() + extras
            )
            logInfo(TAG, "Request body: $requestBody")
            get<JsonHttpRequest>().invoke(
                path = "$BidRequestPath/${adTypeParam.asAdType().code}",
                body = requestBody,
            ).mapCatching { jsonResponse ->
                requireNotNull(JsonParsers.parseOrNull<BidResponse>(jsonResponse))
            }.onFailure {
                logError(TAG, "Error while loading auction data", it)
            }.onSuccess {
                logInfo(TAG, "Loaded auction data: $it")
            }
        }
    }
}

private const val TAG = "BidRequestUseCase"
private const val BidRequestPath = "bidding"