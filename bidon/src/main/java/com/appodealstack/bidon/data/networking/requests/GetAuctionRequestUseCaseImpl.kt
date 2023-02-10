package com.appodealstack.bidon.data.networking.requests

import com.appodealstack.bidon.data.json.JsonParsers
import com.appodealstack.bidon.data.models.auction.*
import com.appodealstack.bidon.data.models.config.AdapterInfo
import com.appodealstack.bidon.data.networking.JsonHttpRequest
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.domain.auction.AdTypeParam
import com.appodealstack.bidon.domain.auction.usecases.GetAuctionRequestUseCase
import com.appodealstack.bidon.domain.common.AdType
import com.appodealstack.bidon.domain.common.BannerSize
import com.appodealstack.bidon.domain.databinders.DataBinderType
import com.appodealstack.bidon.domain.logging.impl.logError
import com.appodealstack.bidon.domain.logging.impl.logInfo
import com.appodealstack.bidon.view.helper.GetOrientationUseCase

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
        val (banner, interstitial, rewarded) = getData(additionalData)
        val adObject = AdObjectRequestBody(
            placementId = placement,
            auctionId = auctionId,
            banner = banner,
            interstitial = interstitial,
            rewarded = rewarded,
            orientationCode = getOrientation().code,
            minPrice = additionalData.priceFloor
        )
        val requestBody = createRequestBody(
            binders = binders,
            dataKeyName = "ad_object",
            data = adObject,
            dataSerializer = JsonParsers.getSerializer()
        )
        logInfo(Tag, "Request body: $requestBody")
        return get<JsonHttpRequest>().invoke(
            path = "$AuctionRequestPath/${additionalData.asAdType().code}",
            body = requestBody,
        ).mapCatching { jsonResponse ->
            requireNotNull(JsonParsers.parseOrNull<AuctionResponse>(jsonResponse.toString()))
        }.onFailure {
            logError(Tag, "Error while loading auction data", it)
        }.onSuccess {
            logInfo(Tag, "Loaded auction data: $it")
        }
    }

    private fun getData(data: AdTypeParam): Triple<BannerRequestBody?, InterstitialRequestBody?, RewardedRequestBody?> {
        return when (data) {
            is AdTypeParam.Banner -> {
                val banner = BannerRequestBody(
                    formatCode = when (data.bannerSize) {
                        BannerSize.Banner -> BannerRequestBody.Format.Banner320x50
                        BannerSize.LeaderBoard -> BannerRequestBody.Format.LeaderBoard728x90
                        BannerSize.MRec -> BannerRequestBody.Format.MRec300x250
                        BannerSize.Adaptive -> BannerRequestBody.Format.AdaptiveBanner320x50
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
