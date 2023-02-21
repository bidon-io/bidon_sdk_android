package com.appodealstack.bidon.auction.impl

import com.appodealstack.bidon.adapter.AdapterInfo
import com.appodealstack.bidon.ads.AdType
import com.appodealstack.bidon.ads.banner.BannerFormat
import com.appodealstack.bidon.ads.banner.helper.GetOrientationUseCase
import com.appodealstack.bidon.auction.AdTypeParam
import com.appodealstack.bidon.auction.models.*
import com.appodealstack.bidon.auction.usecases.GetAuctionRequestUseCase
import com.appodealstack.bidon.databinders.DataBinderType
import com.appodealstack.bidon.logs.logging.impl.logError
import com.appodealstack.bidon.logs.logging.impl.logInfo
import com.appodealstack.bidon.utils.di.get
import com.appodealstack.bidon.utils.json.JsonParsers
import com.appodealstack.bidon.utils.networking.JsonHttpRequest
import com.appodealstack.bidon.utils.networking.requests.CreateRequestBodyUseCase

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
            pricefloor = additionalData.pricefloor
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
            requireNotNull(JsonParsers.parseOrNull<AuctionResponse>(jsonResponse))
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