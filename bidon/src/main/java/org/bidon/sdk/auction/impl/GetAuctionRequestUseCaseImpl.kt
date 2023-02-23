package org.bidon.sdk.auction.impl

import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.helper.GetOrientationUseCase
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.*
import org.bidon.sdk.auction.usecases.GetAuctionRequestUseCase
import org.bidon.sdk.databinders.DataBinderType
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.json.JsonParsers
import org.bidon.sdk.utils.networking.JsonHttpRequest
import org.bidon.sdk.utils.networking.requests.CreateRequestBodyUseCase
import kotlinx.coroutines.withContext

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class GetAuctionRequestUseCaseImpl(
    private val createRequestBody: CreateRequestBodyUseCase,
    private val getOrientation: GetOrientationUseCase,
) : GetAuctionRequestUseCase {
    private val binders: List<DataBinderType> = listOf(
        DataBinderType.AvailableAdapters,
        DataBinderType.Device,
        DataBinderType.App,
        DataBinderType.Token,
        DataBinderType.Geo,
        DataBinderType.Session,
        DataBinderType.User,
        DataBinderType.Segment,
    )

    override suspend fun request(
        placement: String,
        additionalData: AdTypeParam,
        auctionId: String,
        adapters: Map<String, AdapterInfo>,
    ): Result<AuctionResponse> {
        return withContext(SdkDispatchers.IO) {
            val (banner, interstitial, rewarded) = getData(additionalData)
            val adObject = AdObjectRequestBody(
                placementId = placement,
                auctionId = auctionId,
                banner = banner,
                interstitial = interstitial,
                rewarded = rewarded,
                orientationCode = getOrientation().code,
                pricefloor = additionalData.pricefloor
            )
            val requestBody = createRequestBody(
                binders = binders,
                dataKeyName = "ad_object",
                data = adObject,
                dataSerializer = JsonParsers.getSerializer()
            )
            logInfo(Tag, "Request body: $requestBody")
            get<JsonHttpRequest>().invoke(
                path = "$AuctionRequestPath/${additionalData.asAdType().code}",
                body = requestBody,
            ).mapCatching { jsonResponse ->
                requireNotNull(JsonParsers.parseOrNull<AuctionResponse>(jsonResponse))
            }.onFailure {
                logError(Tag, "Error while loading auction data", it)
            }.onSuccess {
                logInfo(Tag, "Loaded auction data: $it")
            }
        }
    }

    private fun getData(data: AdTypeParam): Triple<BannerRequestBody?, InterstitialRequestBody?, RewardedRequestBody?> {
        return when (data) {
            is AdTypeParam.Banner -> {
                val banner = BannerRequestBody(
                    formatCode = when (data.bannerFormat) {
                        BannerFormat.Banner -> BannerRequestBody.Format.Banner320x50
                        BannerFormat.LeaderBoard -> BannerRequestBody.Format.LeaderBoard728x90
                        BannerFormat.MRec -> BannerRequestBody.Format.MRec300x250
                        BannerFormat.Adaptive -> BannerRequestBody.Format.AdaptiveBanner320x50
                    }.code,
                )
                Triple(first = banner, second = null, third = null)
            }
            is AdTypeParam.Interstitial -> {
                Triple(first = null, second = InterstitialRequestBody(), third = null)
            }
            is AdTypeParam.Rewarded -> {
                Triple(first = null, second = null, third = RewardedRequestBody())
            }
        }
    }

    private fun AdTypeParam.asAdType() = when (this) {
        is AdTypeParam.Banner -> AdType.Banner
        is AdTypeParam.Interstitial -> AdType.Interstitial
        is AdTypeParam.Rewarded -> AdType.Rewarded
    }
}

private const val AuctionRequestPath = "auction"
private const val Tag = "AuctionRequestUseCase"