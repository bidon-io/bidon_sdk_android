package com.appodealstack.bidon.auctions.domain.impl

import com.appodealstack.bidon.adapters.banners.BannerSize
import com.appodealstack.bidon.auctions.data.models.*
import com.appodealstack.bidon.auctions.data.models.AdObjectRequestBody
import com.appodealstack.bidon.auctions.data.models.AdTypeAdditional
import com.appodealstack.bidon.auctions.data.models.AuctionResponse
import com.appodealstack.bidon.auctions.domain.GetAuctionRequestUseCase
import com.appodealstack.bidon.auctions.domain.GetOrientationUseCase
import com.appodealstack.bidon.config.data.models.AdapterInfo
import com.appodealstack.bidon.config.domain.DataBinderType
import com.appodealstack.bidon.config.domain.databinders.CreateRequestBodyUseCase
import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInfo
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.utilities.ktor.JsonHttpRequest

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
    )

    override suspend fun request(
        placement: String,
        additionalData: AdTypeAdditional,
        auctionId: String,
        adapters: Map<String, AdapterInfo>
    ): Result<AuctionResponse> {
        val (banner, interstitial, rewarded) = getData(additionalData)
        val adObject = AdObjectRequestBody(
            placementId = placement,
            auctionId = auctionId,
            banner = banner,
            interstitial = interstitial,
            rewarded = rewarded,
            orientationCode = getOrientation().code
        )
        val requestBody = createRequestBody(
            binders = binders,
            dataKeyName = "ad_object",
            data = adObject,
            dataSerializer = AdObjectRequestBody.serializer(),
        )
        logInfo(Tag, "Request body: $requestBody")
        return get<JsonHttpRequest>().invoke(
            path = AuctionRequestPath,
            body = requestBody,
        ).map { jsonResponse ->
            BidonJson.decodeFromJsonElement(AuctionResponse.serializer(), jsonResponse)
        }.onFailure {
            logError(Tag, "Error while loading auction data", it)
        }.onSuccess {
            logInfo(Tag, "Loaded auction data: $it")
        }
    }

    private fun getData(data: AdTypeAdditional): Triple<BannerRequestBody?, InterstitialRequestBody?, RewardedRequestBody?> {
        return when (data) {
            is AdTypeAdditional.Banner -> {
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
            is AdTypeAdditional.Interstitial -> {
                Triple(first = null, second = InterstitialRequestBody(), third = null)
            }
            is AdTypeAdditional.Rewarded -> {
                Triple(first = null, second = null, third = RewardedRequestBody())
            }
        }
    }
}

private const val AuctionRequestPath = "auction"
private const val Tag = "AuctionRequestUseCase"
