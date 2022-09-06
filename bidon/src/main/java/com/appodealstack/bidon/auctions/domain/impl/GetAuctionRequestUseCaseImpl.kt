package com.appodealstack.bidon.auctions.domain.impl

import com.appodealstack.bidon.adapters.banners.BannerSize
import com.appodealstack.bidon.auctions.data.models.AdObjectRequestBody
import com.appodealstack.bidon.auctions.data.models.AdObjectRequestBody.*
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
        val requestBody = createRequestBody.invoke(
            binders = binders,
            adapters = adapters,
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

    private fun getData(data: AdTypeAdditional): Triple<Banner?, Interstitial?, Rewarded?> {
        return when (data) {
            is AdTypeAdditional.Banner -> {
                val banner = Banner(
                    formatCode = when (data.bannerSize) {
                        BannerSize.Banner -> Banner.Format.Banner320x50
                        BannerSize.Large -> Banner.Format.Banner320x50
                        BannerSize.LeaderBoard -> Banner.Format.LeaderBoard728x90
                        BannerSize.MRec -> Banner.Format.MRec300x250
                        BannerSize.Adaptive -> Banner.Format.Banner320x50
                    }.code,
                    adaptive = data.bannerSize == BannerSize.Adaptive
                )
                Triple(first = banner, second = null, third = null)
            }
            is AdTypeAdditional.Interstitial -> {
                val interstitial = Interstitial(
                    formatCodes = Interstitial.Format.values().map { it.code }.toList()
                )
                Triple(first = null, second = interstitial, third = null)
            }
            is AdTypeAdditional.Rewarded -> {
                Triple(first = null, second = null, third = Rewarded())
            }
        }
    }
}

private const val AuctionRequestPath = "auction"
private const val Tag = "AuctionRequestUseCase"
